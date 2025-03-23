package net.ccbluex.liquidbounce.features.command.builder

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParameterContainerTest {
    @Test
    fun testIntoTemplate() {
        val noParam = NoParam.intoTemplate()
        val noParamOptionalTail = noParam.optionalTail

        assertTrue(noParam.requiredParams.isEmpty())
        assertTrue(noParamOptionalTail is CommandParameterTemplate.ParamTail.OptionalParamsTail && noParamOptionalTail.optionalParams.isEmpty())

        val onlyRequiredParam = OnlyRequiredParams.intoTemplate()
        val onlyRequiredOptionalTail = onlyRequiredParam.optionalTail

        assertTrue(onlyRequiredParam.requiredParams[0].name == "A" && onlyRequiredParam.requiredParams[1].name == "B")
        assertTrue(onlyRequiredOptionalTail is CommandParameterTemplate.ParamTail.OptionalParamsTail && onlyRequiredOptionalTail.optionalParams.isEmpty())

        val onlyOptionalParams = OnlyOptionalParams.intoTemplate()
        val onlyOptionalTail = onlyOptionalParams.optionalTail

        assertTrue(onlyOptionalParams.requiredParams.isEmpty())
        assertTrue(onlyOptionalTail is CommandParameterTemplate.ParamTail.OptionalParamsTail && onlyOptionalTail.optionalParams.size == 2 && onlyOptionalTail.optionalParams[0].name == "A" && onlyOptionalTail.optionalParams[1].name == "B")

        val varargParams = VarargParams.intoTemplate()
        val varargOptionalTail = varargParams.optionalTail

        assertTrue(varargParams.requiredParams[0].name == "A")
        assertTrue(varargOptionalTail is CommandParameterTemplate.ParamTail.VarargParamTail && varargOptionalTail.varargParam.name == "B")
    }

    object NoParam: ParameterContainer()
    object OnlyRequiredParams: ParameterContainer() {
        val stringParam = stringParameter("A", required())
        val otherParam = stringParameter("B", required())
    }
    object RequiredParamAfterOptional: ParameterContainer() {
        val stringParam = stringParameter("A", optional())
        val otherParam = stringParameter("B", required())
    }
    object ParamAfterVarargOptional: ParameterContainer() {
        val stringParam = stringParameter("A", required())
        val otherParam = stringParameter("B", vararg())
        val otherParam1 = stringParameter("C", vararg())
    }
    object VarargAfterOptional: ParameterContainer() {
        val stringParam = stringParameter("A", required())
        val otherParam = stringParameter("B", optional())
        val otherParam1 = stringParameter("C", vararg())
    }
    object OnlyOptionalParams: ParameterContainer() {
        val aParam = stringParameter("A", optional())
        val bParam = stringParameter("B", optional())
    }
    object VarargParams: ParameterContainer() {
        val aParam = stringParameter("A", required())
        val bParam = stringParameter("B", vararg())
    }
}
