/**
 */
package language;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>TGG Enum Expression</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link language.TGGEnumExpression#getEenum <em>Eenum</em>}</li>
 *   <li>{@link language.TGGEnumExpression#getLiteral <em>Literal</em>}</li>
 * </ul>
 *
 * @see language.LanguagePackage#getTGGEnumExpression()
 * @model
 * @generated
 */
public interface TGGEnumExpression extends TGGExpression {
	/**
	 * Returns the value of the '<em><b>Eenum</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Eenum</em>' reference.
	 * @see #setEenum(EEnum)
	 * @see language.LanguagePackage#getTGGEnumExpression_Eenum()
	 * @model
	 * @generated
	 */
	EEnum getEenum();

	/**
	 * Sets the value of the '{@link language.TGGEnumExpression#getEenum <em>Eenum</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Eenum</em>' reference.
	 * @see #getEenum()
	 * @generated
	 */
	void setEenum(EEnum value);

	/**
	 * Returns the value of the '<em><b>Literal</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Literal</em>' reference.
	 * @see #setLiteral(EEnumLiteral)
	 * @see language.LanguagePackage#getTGGEnumExpression_Literal()
	 * @model
	 * @generated
	 */
	EEnumLiteral getLiteral();

	/**
	 * Sets the value of the '{@link language.TGGEnumExpression#getLiteral <em>Literal</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Literal</em>' reference.
	 * @see #getLiteral()
	 * @generated
	 */
	void setLiteral(EEnumLiteral value);

} // TGGEnumExpression
