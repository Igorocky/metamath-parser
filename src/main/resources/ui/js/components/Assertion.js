"use strict";

function Assertion ({params,retVal,varColors}) {

    return RE.div(
        {
            style: {fontFamily: 'courier', fontSize: '15px'}
        },
        params.length?RE.ul({},
            params.map((hyp,idx) => RE.li({key:idx},
                re(Expression,{
                    key:`hypotheses-${idx}`,
                    expr:hyp,
                    varColors
                })
            ))
        ):null,
        re(Expression,{
            key:`expression`,
            expr:retVal,
            varColors
        })
    )
}
