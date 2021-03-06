/**
 */
package language;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>TGG Attribute Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link language.TGGAttributeConstraint#getDefinition <em>Definition</em>}</li>
 *   <li>{@link language.TGGAttributeConstraint#getParameters <em>Parameters</em>}</li>
 * </ul>
 *
 * @see language.LanguagePackage#getTGGAttributeConstraint()
 * @model
 * @generated
 */
public interface TGGAttributeConstraint extends EObject {
	/**
	 * Returns the value of the '<em><b>Definition</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Definition</em>' reference.
	 * @see #setDefinition(TGGAttributeConstraintDefinition)
	 * @see language.LanguagePackage#getTGGAttributeConstraint_Definition()
	 * @model
	 * @generated
	 */
	TGGAttributeConstraintDefinition getDefinition();

	/**
	 * Sets the value of the '{@link language.TGGAttributeConstraint#getDefinition <em>Definition</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Definition</em>' reference.
	 * @see #getDefinition()
	 * @generated
	 */
	void setDefinition(TGGAttributeConstraintDefinition value);

	/**
	 * Returns the value of the '<em><b>Parameters</b></em>' reference list.
	 * The list contents are of type {@link language.TGGParamValue}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Parameters</em>' reference list.
	 * @see language.LanguagePackage#getTGGAttributeConstraint_Parameters()
	 * @model
	 * @generated
	 */
	EList<TGGParamValue> getParameters();

} // TGGAttributeConstraint
