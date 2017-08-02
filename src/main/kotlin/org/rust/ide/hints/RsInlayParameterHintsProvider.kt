/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import org.rust.ide.utils.CallInfo
import org.rust.lang.core.psi.RsCallExpr
import org.rust.lang.core.psi.RsLetDecl
import org.rust.lang.core.psi.RsMethodCall
import org.rust.lang.core.psi.RsPatIdent
import org.rust.lang.core.types.ty.TyUnknown
import org.rust.lang.core.types.type
import org.rust.utils.buildList

enum class HintType(desc: String, enabled: Boolean) {
    LET_BINDING_HINT("Show local variable type hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            val element = elem as? RsLetDecl ?: return emptyList()
            if (element.typeReference != null) return emptyList()
            val ident = element.pat as? RsPatIdent ?: return emptyList()
            val patBinding = ident.patBinding
            val type = patBinding.type
            if (type is TyUnknown) return emptyList()
            return listOf(InlayInfo(": " + type.toString(), patBinding.textRange.endOffset))
        }

        override fun isApplicable(elem: PsiElement): Boolean
            = elem is RsLetDecl
    },
    PARAMETER_HINT("Show argument name hints", true) {
        override fun provideHints(elem: PsiElement): List<InlayInfo> {
            val (callInfo, valueArgumentList) = when (elem) {
                is RsCallExpr -> (CallInfo.resolve(elem) to elem.valueArgumentList)
                is RsMethodCall -> (CallInfo.resolve(elem) to elem.valueArgumentList)
                else -> return emptyList()
            }
            if (callInfo == null) return emptyList()

            val hints = buildList<String> {
                if (callInfo.selfParameter != null && elem is RsCallExpr) {
                    add(callInfo.selfParameter)
                }
                addAll(callInfo.parameters.map { "${it.pattern}:" })
            }
            return hints.zip(valueArgumentList.exprList).map { (hint, arg) ->
                InlayInfo(hint, arg.textRange.startOffset)
            }
        }

        override fun isApplicable(elem: PsiElement): Boolean
            = elem is RsCallExpr || elem is RsMethodCall
    };

    companion object {
        fun resolve(elem: PsiElement): HintType?
            = HintType.values().find { it.isApplicable(elem) }

        fun resolveToEnabled(elem: PsiElement?): HintType? {
            val resolved = elem?.let { resolve(it) } ?: return null
            return if (resolved.enabled) {
                resolved
            } else {
                null
            }
        }
    }

    abstract fun isApplicable(elem: PsiElement): Boolean
    abstract fun provideHints(elem: PsiElement): List<InlayInfo>
    val option = Option("SHOW_${this.name}", desc, enabled)
    val enabled get() = option.get()
}

class RsInlayParameterHintsProvider : InlayParameterHintsProvider {
    override fun getSupportedOptions(): List<Option>
        = HintType.values().map { it.option }

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getHintInfo(element: PsiElement?): HintInfo? = null

    override fun getParameterHints(element: PsiElement): List<InlayInfo>
        = HintType.resolveToEnabled(element)?.provideHints(element) ?: emptyList()

    override fun getInlayPresentation(inlayText: String): String = inlayText
}
