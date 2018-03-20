package org.emoflon.ibex.gt.api.generator

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.HashSet

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IFolder
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.emoflon.ibex.gt.editor.gT.GraphTransformationFile

import GTLanguage.GTBindingType
import GTLanguage.GTNode
import GTLanguage.GTParameter
import GTLanguage.GTRule
import GTLanguage.GTRuleSet
import java.util.Set
import java.util.List

/**
 * This GTPackageBuilder implements
 * <ul>
 * <li>transforms the editor files into the internal model and IBeXPatterns</li>
 * <li>and generates code for the API.</li>
 * </ul>
 * 
 * Each package is considered as an rule module with an API.
 */
class JavaFileGenerator {
	/**
	 * The name of the package.
	 */
	String packageName

	/**
	 * The graph transformation rules as instance of the internal GT model.
	 */
	GTRuleSet gtRuleSet

	/**
	 * The mapping between EClass/EDataType names to MetaModelNames
	 */
	HashMap<String, String> eClassifierNameToMetaModelName

	/**
	 * Creates a new JavaFileGenerator.
	 */
	new(String packageName, GTRuleSet gtRuleSet, HashMap<String, String> eClassNameToMetaModelName) {
		this.packageName = packageName
		this.gtRuleSet = gtRuleSet
		this.eClassifierNameToMetaModelName = eClassNameToMetaModelName
	}

	/**
	 * Generates the README.md file.
	 */
	public def generateREADME(IFolder apiPackage, List<IFile> gtFiles, HashSet<String> metaModels,
		HashMap<String, String> metaModelPackages, HashMap<IFile, GraphTransformationFile> editorModels) {
		val debugFileContent = '''
			# «this.packageName»
			You specified «gtRuleSet.rules.size» rules in «gtFiles.size» files.
			
			The generated API is located in `src-gen/«this.packageName».api`.
			
			## Meta-Models
			«FOR metaModel : metaModels»
				- `«metaModel»` (package `«metaModelPackages.get(metaModel)»`)
			«ENDFOR»
			
			## Rules
			«FOR file : gtFiles»
				- File `«file.name»`
					«FOR rule : editorModels.get(file).rules.filter[!it.abstract]»
						- rule `«rule.name»`
					«ENDFOR»
			«ENDFOR»
			
			Note that abstract rules are not included in this list
				because they cannot be applied directly.
			
			## How to specify rules
			1. Add a meta-model reference.
			2. Add the meta-model project(s) as dependency to the `META-INF/MANIFEST.MF`,
				if not done yet (tab *Dependencies* via the button *Add*).
			3. Define your rules by adding `.gt` files into the package.
			
			If there are errors in the specification, you will see this in the editor
				and the generated API may contain errors as well.
			
			### How to use the API in another project
			1. Add the generated packages `«this.packageName».api`, 
				`«this.packageName».api.matches` and `«this.packageName».api.rules`
				to the exported packages of this project
				(tab *Runtime* > *Exported packages* via the button *Add*).
			2. Add this project as a dependency of the project in which you want to use the API.
			3. Create a new API object.
				 ```
				 ResourceSet resourceSet = new ResourceSetImpl();
				 resourceSet.createResource(URI.createFileURI("your-model.xmi"));
				 return new «this.APIClassName»(new DemoclesGTEngine(), resourceSet);
				 ```
		'''
		this.writeFile(apiPackage.getFile("README.md"), debugFileContent)
	}

	/**
	 * Generates the Java API class.
	 */
	public def generateAPIJavaFile(IFolder apiPackage, String patternPath) {
		val rules = this.gtRuleSet.rules.filter[!it.abstract]
		val imports = newHashSet(
			'org.eclipse.emf.common.util.URI',
			'org.eclipse.emf.ecore.resource.ResourceSet',
			'org.emoflon.ibex.common.operational.IContextPatternInterpreter',
			'org.emoflon.ibex.gt.api.GraphTransformationAPI'
		)
		rules.forEach [
			imports.add('''«this.getSubPackageName('api.rules')».«getRuleClassName(it)»''')
			imports.addAll(getImportsForDataTypes(it.parameters))
		]

		val apiClassName = this.APIClassName
		val apiSourceCode = '''
			package «this.getSubPackageName('api')»;
			
			«printImports(imports)»
			
			/**
			 * The «apiClassName».
			 */
			public class «apiClassName» extends GraphTransformationAPI {
				public static String patternPath = "«patternPath»";
			
				/**
				 * Creates a new «apiClassName».
				 *
				 * The are loaded from the default pattern path.
				 *
				 * @param engine
				 *            the engine to use for queries and transformations
				 * @param model
				 *            the resource set containing the model file
				 */
				public «apiClassName»(final IContextPatternInterpreter engine, final ResourceSet model) {
					super(engine, model);
					URI uri = URI.createURI("../" + patternPath);
					this.interpreter.loadPatternSet(uri);
				}
			
				/**
				 * Creates a new «apiClassName».
				 *
				 * The are loaded from the pattern path (the given workspace path concatenated
				 * with the project relative path to the pattern file).
				 *
				 * @param engine
				 *            the engine to use for queries and transformations
				 * @param model
				 *            the resource set containing the model file
				 * @param workspacePath
				 *            the path to the workspace
				 */
				public «apiClassName»(final IContextPatternInterpreter engine, final ResourceSet model,
						final String workspacePath) {
					super(engine, model);
					URI uri = URI.createURI(workspacePath + patternPath);
					this.interpreter.loadPatternSet(uri);
				}
			«FOR rule : rules»
				
					/**
					 * Creates a new rule «getRuleNameAndParameterString(rule)».
					 * 
					 * @return the created rule
					 */
					public «getRuleClassName(rule)» «rule.name»(«FOR parameter : rule.parameters SEPARATOR ', '»final «getJavaType(parameter.type)» «parameter.name»Value«ENDFOR») {
						return new «getRuleClassName(rule)»(this, this.interpreter«FOR parameter : rule.parameters BEFORE ', 'SEPARATOR ', '»«parameter.name»Value«ENDFOR»);
					}
			«ENDFOR»
			}
		'''
		this.writeFile(apiPackage.getFile(apiClassName + '.java'), apiSourceCode)
	}

	/**
	 * Generates the Java Match class for the given rule.
	 */
	public def generateMatchJavaFile(IFolder apiMatchesPackage, GTRule rule) {
		val imports = getImportsForNodeTypes(rule.graph.nodes.filter[!it.local].toList)
		imports.add('org.emoflon.ibex.common.operational.IMatch')
		imports.add('org.emoflon.ibex.gt.api.GraphTransformationMatch')
		imports.add('''«this.getSubPackageName('api.rules')».«getRuleClassName(rule)»''')

		val signatureNodes = rule.graph.nodes.filter[!it.local]
		val matchSourceCode = '''
			package «this.getSubPackageName('api.matches')»;
			
			«printImports(imports)»
			
			/**
			 * A match for the rule «getRuleNameAndParameterString(rule)».
			 */
			public class «getMatchClassName(rule)» extends GraphTransformationMatch<«getMatchClassName(rule)», «getRuleClassName(rule)»> {
				«FOR node : signatureNodes»
					private «getVariableType(node)» «getVariableName(node)»;
				«ENDFOR»
			
				/**
				 * Creates a new match for the rule «rule.name»().
				 * 
				 * @param rule
				 *            the rule
				 * @param match
				 *            the untyped match
				 */
				public «getMatchClassName(rule)»(final «getRuleClassName(rule)» rule, final IMatch match) {
					super(rule, match);
					«FOR node : signatureNodes»
						this.«getVariableName(node)» = («getVariableType(node)») match.get("«node.name»");
					«ENDFOR»
				}
			«FOR node : signatureNodes»
				
					/**
					 * Returns the «node.name».
					 *
					 * @return the «node.name»
					 */
					public «getVariableType(node)» «getMethodName('get', node.name)»() {
						return this.«getVariableName(node)»;
					}
			«ENDFOR»
			}
		'''
		this.writeFile(apiMatchesPackage.getFile(getMatchClassName(rule) + ".java"), matchSourceCode)
	}

	/**
	 * Generates the Java Rule class for the given rule.
	 */
	public def generateRuleJavaFile(IFolder rulesPackage, GTRule rule) {
		val ruleType = if(rule.executable) 'GraphTransformationApplicableRule' else 'GraphTransformationRule'
		val parameterNodes = rule.graph.nodes.filter[it.bindingType != GTBindingType.CREATE && !it.local].toList
		val imports = getImportsForNodeTypes(parameterNodes)
		imports.addAll(getImportsForDataTypes(rule.parameters))
		imports.addAll(
			'java.util.ArrayList',
			'java.util.List',
			'org.emoflon.ibex.common.operational.IMatch',
			'''org.emoflon.ibex.gt.api.«ruleType»''',
			'org.emoflon.ibex.gt.engine.GraphTransformationInterpreter',
			'''«this.getSubPackageName('api')».«APIClassName»''',
			'''«this.getSubPackageName('api.matches')».«getMatchClassName(rule)»'''
		)
		if (rule.parameters.size > 0 || parameterNodes.size > 0) {
			imports.add('java.util.Objects');
		}

		val ruleSourceCode = '''
			package «this.getSubPackageName('api.rules')»;
			
			«printImports(imports)»
			
			/**
			 * The rule «getRuleNameAndParameterString(rule)».
			 */
			public class «getRuleClassName(rule)» extends «ruleType»<«getMatchClassName(rule)», «getRuleClassName(rule)»> {
				private static String ruleName = "«rule.name»";
			
				/**
				 * Creates a new rule «rule.name»(«FOR parameter : rule.parameters SEPARATOR ', '»«parameter.name»«ENDFOR»).
				 * 
				 * @param api
				 *            the API the rule belongs to
				 * @param interpreter
				 *            the interpreter
				 «FOR parameter : rule.parameters»
				 	* @param «parameter.name»Value
				 	*            the value for the parameter «parameter.name»
				 «ENDFOR»
				 */
				public «getRuleClassName(rule)»(final «APIClassName» api, final GraphTransformationInterpreter interpreter«IF rule.parameters.size == 0») {«ELSE»,«ENDIF»
						«FOR parameter : rule.parameters SEPARATOR ', ' AFTER ') {'»final «getJavaType(parameter.type)» «parameter.name»Value«ENDFOR»
					super(api, interpreter, ruleName);
					«FOR parameter : rule.parameters»
						this.«getMethodName('set', parameter.name)»(«parameter.name»Value);
					«ENDFOR»
				}
			
				@Override
				protected «getMatchClassName(rule)» convertMatch(final IMatch match) {
					return new «getMatchClassName(rule)»(this, match);
				}
			
				@Override
				protected List<String> getParameterNames() {
					List<String> names = new ArrayList<String>();
					«FOR node : parameterNodes»
						names.add("«node.name»");
					«ENDFOR»
					return names;
				}
			«FOR node : parameterNodes»
				
					/**
					 * Binds the parameter «node.name» to the given object.
					 *
					 * @param object
					 *            the object to set
					 */
					public «getRuleClassName(rule)» «getMethodName('bind', node.name)»(final «getVariableType(node)» object) {
						this.parameters.put("«node.name»", Objects.requireNonNull(object, "«node.name» must not be null!"));
						return this;
					}
			«ENDFOR»
			«FOR parameter : rule.parameters»
				
					/**
					 * Sets the parameter «parameter.name» to the given value.
					 *
					 * @param value
					 *            the value to set
					 */
					public «getRuleClassName(rule)» «getMethodName('set', parameter.name)»(final «getJavaType(parameter.type)» value) {
						this.parameters.put("«parameter.name»", Objects.requireNonNull(value, "«parameter.name» must not be null!"));
						return this;
					}
			«ENDFOR»
			}
		'''
		this.writeFile(rulesPackage.getFile(getRuleClassName(rule) + ".java"), ruleSourceCode)
	}

	/**
	 * Determines the set of necessary type imports for a set of nodes.
	 */
	private def getImportsForNodeTypes(List<GTNode> nodes) {
		return getImportsForTypes(nodes.map[it.type])
	}

	/**
	 * Determines the set of necessary type imports for the parameters.
	 */
	private def getImportsForDataTypes(List<GTParameter> parameters) {
		return getImportsForTypes(parameters.map[it.type])
	}

	/**
	 * Determines the set of necessary imports for the given EClassifiers.
	 */
	private def getImportsForTypes(List<? extends EClassifier> types) {
		val imports = newHashSet()
		types.toSet.forEach [
			val typePackageName = this.eClassifierNameToMetaModelName.get(it.name)
			if (typePackageName !== null) {
				imports.add(typePackageName + '.' + it.name)
			}
		]
		return imports.sortBy[it].toSet
	}

	/**
	 * Sub template for Java import statements
	 */
	private static def printImports(Set<String> imports) {
		return '''
			«FOR importClass : imports.sortBy[it.toLowerCase]»
				import «importClass»;
			«ENDFOR»
		'''
	}

	/**
	 * Returns the name of the package.
	 */
	private def getSubPackageName(String subPackage) {
		val dot = if(this.packageName.equals("")) "" else "."
		return '''«this.packageName»«dot»«subPackage»'''
	}

	/**
	 * Returns the name of the API class.
	 */
	private def getAPIClassName() {
		return this.packageName.replace('.', '').toFirstUpper + "API"
	}

	/**
	 * Returns the name of the match class for the rule.
	 */
	private static def getMatchClassName(GTRule rule) {
		return rule.name.toFirstUpper + "Match"
	}

	/**
	 * Returns the name of the rule class for the rule.
	 */
	private static def getRuleClassName(GTRule rule) {
		return rule.name.toFirstUpper + "Rule"
	}

	/**
	 * Returns the concatenation of rule name and the list of parameter names.
	 */
	private static def getRuleNameAndParameterString(GTRule rule) {
		return '''«rule.name»(«FOR parameter : rule.parameters SEPARATOR ', '»«parameter.name»«ENDFOR»)'''
	}

	/**
	 * Returns the getter method name for the given name.
	 */
	private static def getMethodName(String prefix, String name) {
		return prefix + name.toFirstUpper
	}

	/**
	 * Returns the variable name for the given node.
	 */
	private static def getVariableName(GTNode node) {
		return 'var' + node.name.toFirstUpper
	}

	/**
	 * Returns the name of the type of given node.
	 */
	private static def getVariableType(GTNode node) {
		return node.type.name
	}

	/**
	 * Returns the equivalent Java type for the EDataType.
	 */
	private static def getJavaType(EDataType dataType) {
		return if(dataType instanceof EEnum) dataType.name else dataType.instanceTypeName
	}

	/**
	 * Creates the file containing the content.
	 */
	private def writeFile(IFile file, String content) {
		val contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
		if (file.exists) {
			file.setContents(contentStream, true, true, null)
		} else {
			file.create(contentStream, true, null)
		}
	}
}
