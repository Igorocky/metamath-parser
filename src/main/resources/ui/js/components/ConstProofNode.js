"use strict";

function ConstProofNode ({node, varColors}) {

    return RE.span({style:{fontFamily:'courier', fontSize:'15px'}},
        re(Expression,{key:`expr-${node.id}`,expr:node.expr,varColors})
    )
}
