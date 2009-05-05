//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.04.21 at 03:54:44 PM BRT 
//


package br.com.sysmap.crux.core.rebind.screen.config.parser;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for parserType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="parserType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="dom"/>
 *     &lt;enumeration value="jericho"/>
 *     &lt;enumeration value="string"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "parserType")
@XmlEnum
public enum ParserType {

    @XmlEnumValue("dom")
    DOM("dom"),
    @XmlEnumValue("jericho")
    JERICHO("jericho"),
    @XmlEnumValue("string")
    STRING("string");
    private final String value;

    ParserType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ParserType fromValue(String v) {
        for (ParserType c: ParserType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}