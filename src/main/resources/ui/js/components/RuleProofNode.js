"use strict";

function RuleProofNode({node,allNodes,varColors,hideTypes,stepNumbers}) {

    function renderArgAndParam({key,ex,centerX,argIdx,arg,param,subs,swapSubs,subsColors,varColors}) {
        const argStr = arg.join(' ')
        const paramStr = param.join(' ')

        let argBottomLineEx = ex
        let paramBottomLineEx = ex.translate(ex.rotate(-90),6*charHeight)

        const argLength = argStr.length*charLength
        const paramLength = paramStr.length*charLength

        if (hasValue(centerX)) {
            argBottomLineEx = argBottomLineEx.translate(null, centerX - argLength / 2 - argBottomLineEx.start.x)
            paramBottomLineEx = paramBottomLineEx.translate(null, centerX - paramLength / 2 - paramBottomLineEx.start.x)
        } else {
            if (argLength < paramLength) {
                argBottomLineEx = argBottomLineEx.translate(null,(paramLength-argLength)/2)
            } else {
                paramBottomLineEx = paramBottomLineEx.translate(null,(argLength-paramLength)/2)
            }
        }

        const argBottomLine = argBottomLineEx.scale(argLength)
        const argLeftLine = argBottomLineEx.rotate(90).scale(charHeight)
        const paramBottomLine = paramBottomLineEx.scale(paramLength)
        const paramLeftLine = paramBottomLineEx.rotate(90).scale(charHeight)

        const argBoundaries = SvgBoundaries.fromPoints(argLeftLine.end,argBottomLine.end).addAbsoluteMargin(SCALE*0.5)
        const paramBoundaries = SvgBoundaries.fromPoints(paramLeftLine.end,paramBottomLine.end).addAbsoluteMargin(SCALE*0.5)
        const argIdxTextStart = new Point(argBottomLine.start.x, argBottomLine.start.y-charHeight*3)
        return {
            svgElems: [
                hasValue(argIdx)?SVG.text({
                        key:`${key}-arg-idx`,
                        x:argIdxTextStart.x,
                        y:argIdxTextStart.y,
                        fill:'black',
                        fontSize:fontSizePx,
                        fontFamily,
                        style:{cursor: 'pointer'},
                        onClick: () => {
                            location.hash = "#" + argIdx
                        }
                    },
                    stepNumbers[argIdx]
                ):null,
                ...determineSubsIndexes({param:swapSubs?arg:param,subs,subsColors}).flatMap((idxMapping,idx) => renderMapping({
                    key:`${key}-mapping-${idx}`,
                    argEx: swapSubs?paramBottomLineEx:argBottomLineEx,
                    paramEx: swapSubs?argBottomLineEx:paramBottomLineEx,
                    idxMapping,
                    swapSubs
                })),
                ...(renderColoredExpr({key:`${key}-arg-text`,ex:argBottomLine.normalize(),expr:arg,colors:varColors}).svgElems),
                ...(renderColoredExpr({key:`${key}-param-text`,ex:paramBottomLineEx.normalize(),expr:param,colors:varColors}).svgElems)
            ],
            boundaries: mergeSvgBoundaries(argBoundaries,paramBoundaries)
                .addPoints(argIdxTextStart.withY(oldY => oldY-charHeight*2))
                .addPoints(paramBottomLineEx.start.withY(oldY => oldY+charHeight*2)),
            argBoundaries,
            paramBoundaries
        }
    }

    function incY({boundaries,dy}) {
        return new SvgBoundaries(
            boundaries.minX, boundaries.maxX,
            boundaries.minY-dy, boundaries.maxY+dy,
        )
    }

    function renderMapping({key, argEx, paramEx, idxMapping, swapSubs}) {
        const argBottom = argEx.translate(null, charLength*idxMapping.argBeginIdx).scale(charLength*(idxMapping.argEndIdx-idxMapping.argBeginIdx+1))
        const argLeft = argEx.translateTo(argBottom.start).rotate(90).scale(charHeight)

        const paramBottom = paramEx.translate(null, charLength*idxMapping.paramBeginIdx).scale(charLength*(idxMapping.paramEndIdx-idxMapping.paramBeginIdx+1))
        const paramLeft = paramEx.translateTo(paramBottom.start).rotate(90).scale(charHeight)

        const dy = charHeight*1.2
        const argBoundaries = incY({boundaries:SvgBoundaries.fromPoints(argLeft.end, argBottom.end),dy})
        const paramBoundaries = incY({boundaries:SvgBoundaries.fromPoints(paramLeft.end, paramBottom.end),dy:dy*0.4})
        return [
            svgPolygon({
                key:`${key}-arg`,
                boundaries: argBoundaries,
                props: {fill:'none', stroke:idxMapping.color, strokeWidth: SCALE*0.1}
            }),
            new Vector(
                new Point((argBoundaries.minX + argBoundaries.maxX)/2, swapSubs?argBoundaries.minY:argBoundaries.maxY),
                new Point((paramBoundaries.minX + paramBoundaries.maxX)/2, swapSubs?paramBoundaries.maxY:paramBoundaries.minY)
            ).toSvgLine({
                key:`${key}-line`,
                props: {fill:'none', stroke:idxMapping.color, strokeWidth: SCALE*0.1}
            })
        ]
    }

    function calcSymLengths({symbols}) {
        return symbols.reduce(
            (acc,sym) => acc === null
                ? [{begin:0,end:sym.length-1,len:sym.length}]
                : [...acc, {begin:acc.last().end+2,end:acc.last().end+sym.length+1,len:sym.length}],
            null
        )
    }

    function determineSubsIndexes({param,subs,subsColors}) {
        const arg = []
        const symIdxMapping = []
        for (let i = 0; i < param.length; i++) {
            const subExp = subs[param[i]];
            if (subExp) {
                symIdxMapping.push({
                    color: subsColors[param[i]],
                    paramIdx: i,
                    argBeginIdx:arg.length,
                    argEndIdx:arg.length + subExp.length - 1,
                })
                arg.push(...subExp)
            } else {
                arg.push(param[i])
            }
        }
        const argSymLen = calcSymLengths({symbols:arg})
        const paramSymLen = calcSymLengths({symbols:param})
        return symIdxMapping.map(m => ({
            color: m.color,
            argBeginIdx: argSymLen[m.argBeginIdx].begin,
            argEndIdx: argSymLen[m.argEndIdx].end,
            paramBeginIdx: paramSymLen[m.paramIdx].begin,
            paramEndIdx: paramSymLen[m.paramIdx].end,
        }))
    }

    function renderAllParams({ex}) {
        const resultSvgElems = []
        let resultBoundaries = null
        let resultParamBoundaries = null

        const subsColors = Object.getOwnPropertyNames(node.substitution)
            .map((name,idx) => ({name,color:idx<subsAvailableColors.length?subsAvailableColors[idx]:'black'}))
            .reduce((acc,elem) => ({...acc,[elem.name]:elem.color}), {})

        let lastEx = ex
        for (let i = hideTypes?node.numOfTypes:0; i < node.params.length; i++) {
            const {svgElems, boundaries, paramBoundaries} = renderArgAndParam({
                key: `argAndParam-${i}`,
                ex: lastEx,
                argIdx:node.args[i],
                arg: allNodes[node.args[i]].expr,
                param: node.params[i],
                subs: node.substitution,
                subsColors,
                varColors
            })
            resultSvgElems.push(...svgElems)
            lastEx = lastEx.translate(null, (boundaries.maxX - boundaries.minX) + charLength*5)
            resultBoundaries = mergeSvgBoundaries(resultBoundaries, boundaries)
            resultParamBoundaries = mergeSvgBoundaries(resultParamBoundaries, paramBoundaries)
        }

        const {svgElems, boundaries, argBoundaries} = renderArgAndParam({
            key: `assertion`,
            ex: !hasValue(resultBoundaries) ? ex : ex.translate(ex.rotate(-90), resultBoundaries.maxY-ex.start.y + charHeight*4),
            centerX: !hasValue(resultBoundaries) ? undefined : (resultParamBoundaries.minX + resultParamBoundaries.maxX)/2,
            arg: node.retVal,
            param: node.expr,
            subs: node.substitution,
            swapSubs:true,
            subsColors,
            varColors
        })
        resultSvgElems.push(...svgElems)
        resultBoundaries = mergeSvgBoundaries(resultBoundaries, boundaries)

        const ruleBoundaries = mergeSvgBoundaries(resultParamBoundaries, argBoundaries)
        const midLineProps = {fill:'none', stroke:'black', strokeWidth: SCALE*0.1}
        let ruleMidY
        if (resultParamBoundaries) {
            ruleMidY = (ruleBoundaries.minY + ruleBoundaries.maxY) / 2
            resultSvgElems.push(svgLine({
                key: `rule-line`,
                from: new Point(ruleBoundaries.minX, ruleMidY),
                to: new Point(ruleBoundaries.maxX, ruleMidY),
                props:midLineProps
            }))
        } else {
            ruleMidY = argBoundaries.minY - charHeight
            resultSvgElems.push(svgLine({
                key: `rule-line`,
                from: new Point(argBoundaries.minX, ruleMidY),
                to: new Point(argBoundaries.maxX, ruleMidY),
                props:midLineProps
            }))
            resultBoundaries = resultBoundaries.addPoints(new Point(argBoundaries.minX, ruleMidY-charHeight))
        }

        return {svgElems: resultSvgElems, boundaries: resultBoundaries}
    }

    const {svgElems, boundaries} = renderAllParams({ex:SVG_EX})

    return RE.svg(
        {
            width: boundaries.width()/pxSize,
            height: boundaries.height()/pxSize,
            boundaries: boundaries,
        },
        // SVG.rect({key:'background', x:-100000, y:-100000, width:200000, height:200000, fill:'lightyellow'}),
        svgElems,
        // boundaries.toRect({key:`boundaries`,color:'red'})
    )
}
