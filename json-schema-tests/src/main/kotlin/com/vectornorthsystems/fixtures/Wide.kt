package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * A non-recursive class used twice, defined once in `$defs` and referenced twice.
 */
@SchemaRoot
data class Customer(
    val billing: Address,
    val shipping: Address,
)

data class Address(
    val street: String = "",
    val city: String = "",
)

/**
 * More than 32 sibling classes, which are wide rather than deep, so they must not hit the depth limit.
 */
@SchemaRoot
data class Wide(
    val p0: Wide0,
    val p1: Wide1,
    val p2: Wide2,
    val p3: Wide3,
    val p4: Wide4,
    val p5: Wide5,
    val p6: Wide6,
    val p7: Wide7,
    val p8: Wide8,
    val p9: Wide9,
    val p10: Wide10,
    val p11: Wide11,
    val p12: Wide12,
    val p13: Wide13,
    val p14: Wide14,
    val p15: Wide15,
    val p16: Wide16,
    val p17: Wide17,
    val p18: Wide18,
    val p19: Wide19,
    val p20: Wide20,
    val p21: Wide21,
    val p22: Wide22,
    val p23: Wide23,
    val p24: Wide24,
    val p25: Wide25,
    val p26: Wide26,
    val p27: Wide27,
    val p28: Wide28,
    val p29: Wide29,
    val p30: Wide30,
    val p31: Wide31,
    val p32: Wide32,
    val p33: Wide33,
    val p34: Wide34,
    val p35: Wide35,
    val p36: Wide36,
    val p37: Wide37,
    val p38: Wide38,
    val p39: Wide39,
)

data class Wide0(val value: String = "")
data class Wide1(val value: String = "")
data class Wide2(val value: String = "")
data class Wide3(val value: String = "")
data class Wide4(val value: String = "")
data class Wide5(val value: String = "")
data class Wide6(val value: String = "")
data class Wide7(val value: String = "")
data class Wide8(val value: String = "")
data class Wide9(val value: String = "")
data class Wide10(val value: String = "")
data class Wide11(val value: String = "")
data class Wide12(val value: String = "")
data class Wide13(val value: String = "")
data class Wide14(val value: String = "")
data class Wide15(val value: String = "")
data class Wide16(val value: String = "")
data class Wide17(val value: String = "")
data class Wide18(val value: String = "")
data class Wide19(val value: String = "")
data class Wide20(val value: String = "")
data class Wide21(val value: String = "")
data class Wide22(val value: String = "")
data class Wide23(val value: String = "")
data class Wide24(val value: String = "")
data class Wide25(val value: String = "")
data class Wide26(val value: String = "")
data class Wide27(val value: String = "")
data class Wide28(val value: String = "")
data class Wide29(val value: String = "")
data class Wide30(val value: String = "")
data class Wide31(val value: String = "")
data class Wide32(val value: String = "")
data class Wide33(val value: String = "")
data class Wide34(val value: String = "")
data class Wide35(val value: String = "")
data class Wide36(val value: String = "")
data class Wide37(val value: String = "")
data class Wide38(val value: String = "")
data class Wide39(val value: String = "")
