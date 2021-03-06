/**
 */
package IBeXLanguage;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>IBe XContext Alternatives</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link IBeXLanguage.IBeXContextAlternatives#getAlternativePatterns <em>Alternative Patterns</em>}</li>
 * </ul>
 *
 * @see IBeXLanguage.IBeXLanguagePackage#getIBeXContextAlternatives()
 * @model
 * @generated
 */
public interface IBeXContextAlternatives extends IBeXContext {
	/**
	 * Returns the value of the '<em><b>Alternative Patterns</b></em>' containment reference list.
	 * The list contents are of type {@link IBeXLanguage.IBeXContextPattern}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Alternative Patterns</em>' containment reference list.
	 * @see IBeXLanguage.IBeXLanguagePackage#getIBeXContextAlternatives_AlternativePatterns()
	 * @model containment="true"
	 * @generated
	 */
	EList<IBeXContextPattern> getAlternativePatterns();

} // IBeXContextAlternatives
