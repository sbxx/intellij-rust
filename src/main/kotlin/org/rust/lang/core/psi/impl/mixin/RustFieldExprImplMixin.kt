package org.rust.lang.core.psi.impl.mixin

import com.intellij.lang.ASTNode
import org.rust.lang.core.psi.RustFieldExprElement
import org.rust.lang.core.psi.impl.RustExprElementImpl
import org.rust.lang.core.resolve.ref.RustFieldExprReferenceImpl
import org.rust.lang.core.resolve.ref.RustReference

abstract class RustFieldExprImplMixin(node: ASTNode?) : RustExprElementImpl(node), RustFieldExprElement {

    override fun getReference(): RustReference? = fieldId?.let { RustFieldExprReferenceImpl(this) }

}