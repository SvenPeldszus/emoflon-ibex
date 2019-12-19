/**
 */
package language.repair;

import language.TGGRuleElement;

import org.eclipse.emf.ecore.EObject;
// <-- [user defined imports]
// [user defined imports] -->

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>TGG Rule Element Mapping</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link language.repair.TGGRuleElementMapping#getSourceRuleElement <em>Source Rule Element</em>}</li>
 *   <li>{@link language.repair.TGGRuleElementMapping#getTargetRuleElement <em>Target Rule Element</em>}</li>
 * </ul>
 * </p>
 *
 * @see language.repair.RepairPackage#getTGGRuleElementMapping()
 * @model
 * @generated
 */
public interface TGGRuleElementMapping extends EObject {
	/**
	 * Returns the value of the '<em><b>Source Rule Element</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Source Rule Element</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Source Rule Element</em>' reference.
	 * @see #setSourceRuleElement(TGGRuleElement)
	 * @see language.repair.RepairPackage#getTGGRuleElementMapping_SourceRuleElement()
	 * @model
	 * @generated
	 */
	TGGRuleElement getSourceRuleElement();

	/**
	 * Sets the value of the '{@link language.repair.TGGRuleElementMapping#getSourceRuleElement <em>Source Rule Element</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source Rule Element</em>' reference.
	 * @see #getSourceRuleElement()
	 * @generated
	 */
	void setSourceRuleElement(TGGRuleElement value);

	/**
	 * Returns the value of the '<em><b>Target Rule Element</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Target Rule Element</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Rule Element</em>' reference.
	 * @see #setTargetRuleElement(TGGRuleElement)
	 * @see language.repair.RepairPackage#getTGGRuleElementMapping_TargetRuleElement()
	 * @model
	 * @generated
	 */
	TGGRuleElement getTargetRuleElement();

	/**
	 * Sets the value of the '{@link language.repair.TGGRuleElementMapping#getTargetRuleElement <em>Target Rule Element</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Rule Element</em>' reference.
	 * @see #getTargetRuleElement()
	 * @generated
	 */
	void setTargetRuleElement(TGGRuleElement value);
	// <-- [user code injected with eMoflon]

	// [user code injected with eMoflon] -->
} // TGGRuleElementMapping