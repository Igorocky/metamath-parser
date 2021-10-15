"use strict";

const SCALE = 10
const pxSize = 1.5
const fontFamily = 'courier'
const fontSize = (SCALE*2)
const fontSizePx = fontSize+'px'
const charLength = fontSize*0.6
const charHeight = charLength*0.85
const subsAvailableColors = ['green', 'orange', '#03a9f4', 'pink', 'brown', 'lawngreen', 'olive', 'blue', 'red', 'magenta']
const typeColors = {wff:'blue',term:'black',setvar:'red',['class']:'magenta'}
const NBSP = String.fromCodePoint(160)

function createVarColors({varTypes}) {
    return Object.getOwnPropertyNames(varTypes)
        .map(varName => [varName,typeColors[varTypes[varName]]??'gold'])
        .reduce((acc,[varName,color]) => ({...acc, [varName]:color}), {})
}

function applyLinks({text}) {
    //the line below doesn't work in Safari
    // const matches = text.matchAll(/(?<!~)(~\s(\S+))/g)

    text = ' ' + text
    const matches = text.matchAll(/([^~])(~\s(\S+))/g)

    const resultContent = []
    let lastIdx = 0
    for (const match of matches) {
        if (match.index > 0) {
            resultContent.push(text.substring(lastIdx,match.index))
        }

        const matchedText = match[3].replaceAll('~~','~')
        resultContent.push(match[1])
        resultContent.push(
            RE.a({href:matchedText.startsWith('http')?matchedText:createUrlOfAssertion(matchedText)},
                matchedText
            ),
        )
        lastIdx = match.index + match[0].length
    }
    if (lastIdx < text.length - 1) {
        resultContent.push(text.substring(lastIdx))
    }
    if (resultContent.length) {
        resultContent[0] = resultContent[0].substring(1)
    }
    return RE.Fragment({},resultContent)
}

function renderColoredExpr({key,ex,expr,colors}) {
    const svgElems = []
    let numOfChars = 0
    let curEx = ex;
    for (let i = 0; i < expr.length; i++) {
        const text = expr[i]
        svgElems.push(SVG.text({
                key:`${key}-${i}`,
                x:curEx.start.x,
                y:curEx.start.y,
                fill:colors[text]??'black',
                fontSize:fontSizePx,
                fontFamily,
                fontWeight:colors[text]?'900':'none'
            },
            text
        ))
        numOfChars += text.length+1
        curEx = curEx.translate(null,charLength*(text.length+1))
    }
    if (expr.length) {
        numOfChars -= 1
    }
    const bottomLine = ex.scale(numOfChars*charLength)
    return {svgElems, boundaries: SvgBoundaries.fromPoints(bottomLine.start, bottomLine.end.withY(y => y - charHeight))}
}

const POSSIBLE_PARENTHESES_PAIRS = [
    ['(',')'],
    ['[',']'],
    ['{','}'],
    ['<.','>.'],
]

function useParenthesesHighlighting({expr}) {
    const matchingParenIndexes = useMemo(() => {
        let result = {}
        const parenStack = []
        for (let i = 0; i < expr.length; i++) {
            const sym = expr[i]
            const opening = isOpeningParen(sym)
            if (hasValue(opening)) {
                parenStack.push({idx:i,paren:sym})
            } else {
                const closing = isClosingParen(sym)
                if (hasValue(closing)) {
                    if (parenStack.length == 0) {
                        console.log('parenStack.length == 0')
                    } else if (!isMatching(parenStack[parenStack.length-1].paren, closing)) {
                        console.log('!isMatching(parenStack[parenStack.length-1].paren, closing)')
                    } else {
                        const opening = parenStack.pop()
                        result = {...result, [opening.idx]:i, [i]:opening.idx}
                    }
                }
            }
        }
        return result
    })

    const [pinnedIndexes, setPinnedIndexes] = useState({})
    const [hoveredOverIndexes, setHoveredOverIndexes] = useState({})

    function isOpeningParen(paren) {
        return POSSIBLE_PARENTHESES_PAIRS.find(([o,c]) => o === paren)?.[0]
    }

    function isClosingParen(paren) {
        return POSSIBLE_PARENTHESES_PAIRS.find(([o,c]) => c === paren)?.[1]
    }

    function isMatching(opening,closing) {
        return POSSIBLE_PARENTHESES_PAIRS.find(([o,c]) => o === opening)?.[1] === closing
    }

    function isParenthesis(sym) {
        return POSSIBLE_PARENTHESES_PAIRS.findIndex(([o,c]) => o === sym || c === sym) >= 0
    }

    return {
        onMouseEnter: idx => {
            const matchingParenIdx = matchingParenIndexes[idx]
            if (hasValue(matchingParenIdx)) {
                setHoveredOverIndexes({[idx]:'orange',[matchingParenIdx]:'orange'})
            }
        },
        onMouseLeave: () => setHoveredOverIndexes({}),
        getBackgroundColor: idx => hoveredOverIndexes[idx]??pinnedIndexes[idx],
        isParenthesis,
        isPinned: idx => hasValue(pinnedIndexes[idx]),
        pin: ({idx,color}) => {
            const matchingParenIdx = matchingParenIndexes[idx]
            if (hasValue(matchingParenIdx)) {
                setPinnedIndexes(prev => ({...prev, [idx]:color, [matchingParenIdx]:color}))
            }
        },
        unpin: idx => {
            const matchingParenIdx = matchingParenIndexes[idx]
            if (hasValue(matchingParenIdx)) {
                setPinnedIndexes(prev => ({...prev, [idx]:undefined, [matchingParenIdx]:undefined}))
            }
        }
    }
}

function getTextWidth(text, font) {
    const canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"))
    const context = canvas.getContext("2d")
    context.font = font
    const metrics = context.measureText(text)
    return metrics.width
}