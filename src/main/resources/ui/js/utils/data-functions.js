'use strict';

function hasValue(variable) {
    return variable !== undefined && variable !== null
}

function hasNoValue(variable) {
    return !hasValue(variable)
}

function nvl(...args) {
    for (let i = 0; i < arg.length; i++) {
        const arg = args[i];
        if (hasValue(arg)) {
            return arg
        }
    }
    return undefined
}

function isObject(obj) {
    return typeof obj === 'object' && !Array.isArray(obj)
}

function randomInt(min, max) {
    return min + Math.floor(Math.random()*((max-min)+1))
}

function ints(start, end) {
    let i = start
    const res = [];
    while (i <= end) {
        res.push(i)
        i++
    }
    return res
}

function prod(...arrays) {
    if (arrays.length) {
        const childProdResult = prod(...arrays.rest());
        return arrays.first().flatMap(e => childProdResult.map(row => [e,...row]))
    } else {
        return [[]]
    }
}

Array.prototype.min = function () {
    return this.reduce((a,b) => hasValue(a)?(hasValue(b)?(Math.min(a,b)):a):b)
}

Array.prototype.max = function () {
    return this.reduce((a,b) => hasValue(a)?(hasValue(b)?(Math.max(a,b)):a):b)
}

Array.prototype.sum = function () {
    return this.reduce((a,b) => a+b, 0)
}

Array.prototype.attr = function(...attrs) {
    if (attrs.length > 1) {
        return this.map(e => attrs.reduce((o,a)=>({...o,[a]:e[a]}), {}))
    } else {
        return this.map(e => e[attrs[0]])
    }
}

Array.prototype.first = function() {
    return this[0]
}

Array.prototype.last = function() {
    return this[this.length-1]
}

Array.prototype.rest = function() {
    return this.filter((e,idx) => 0 < idx)
}

function inc(arr, idx) {
    return modifyAtIdx(arr, idx, i => i+1)
}

function modifyAtIdx(arr, idx, modifier) {
    return arr.map((e,i) => i==idx?modifier(e):e)
}

function removeAtIdx(arr, idx) {
    return arr.filter((e,i) => i!=idx)
}

function nextRandomElem({allElems,counts}) {
    const elemsWithCnt = allElems.map(elem => ({...elem, cnt:counts[elem.idx]}))
    const minCnt = elemsWithCnt.attr('cnt').min()
    const elemsWithMinCnt = elemsWithCnt.filter(elem => elem.cnt == minCnt)
    return elemsWithMinCnt[randomInt(0,elemsWithMinCnt.length-1)]
}

function createObj(obj) {
    const self = {
        ...obj,
        set: (attr, value) => {
            // console.log(`Setting in object: attr = ${attr}, value = ${value}`)
            return createObj({...obj, [attr]: value})
        },
        attr: (...attrs) => attrs.reduce((o,a)=>({...o,[a]:obj[a]}), {}),
        map: mapper => {
            const newObj = mapper(self)
            if (isObject(newObj)) {
                return createObj(newObj)
            } else {
                return newObj
            }
        }
    }
    return self
}

function objectHolder(obj) {
    return {
        get: (attr) => attr?obj[attr]:obj,
        set: (attr, value) => {
            // console.log(`Setting in holder: attr = ${attr}, value = ${value}`)
            obj = obj.set(attr, value)
        },
        setObj: (newObj) => {
            obj = newObj
        },
        attr: (...attrs) => obj.attr(...attrs),
        map: mapper => obj = obj.map(mapper),
    }
}

function saveToLocalStorage(localStorageKey, value) {
    try {
        window.localStorage.setItem(localStorageKey, JSON.stringify(value))
    } catch (e) {
        //do nothing
    }
}

function readFromLocalStorage(localStorageKey, defaultValue) {
    try {
        const item = window.localStorage.getItem(localStorageKey)
        return hasValue(item) ? JSON.parse(item) : defaultValue
    } catch (e) {
        return defaultValue
    }
}

function replaceDots(str) {
    return str.replaceAll('.','-dot-')
}

function getRelPath({label}) {
    if (label.length >= 6) {
        return [label.substring(0,2), label.substring(2,4), label.substring(4,6)]
    } else if (label.length >= 4) {
        return [label.substring(0,2), label.substring(2,4)]
    } else if (label.length >= 2) {
        return [label.substring(0,2)]
    } else {
        return [label]
    }
}

function createUrlOfAssertion(label) {
    // return version + relPathToRoot + 'data/' + getRelPath({label}).map(replaceDots).join('/') + '/' + replaceDots(label) + '.html'
    return relPathToRoot + `/asrt/${label}.html`
}

function decompressAssertionDto(cDto) {
    const strings = cDto.s.split(' ')
    return {
        type: cDto.t,
        name: cDto.n,
        description: cDto.d,
        varTypes: decompressVarTypes(decompressMapOfInts(cDto.v),strings),
        params: (cDto.pa==='')?[]:cDto.pa?.split(' ')?.map(param => listOfIntsToListOfStrings(decompressListOfInts(param), strings)),
        retVal: listOfIntsToListOfStrings(decompressListOfInts(cDto.r), strings),
        proof: cDto.p?.map(n => decompressStackNodeDto(n, strings))
    }
}

function decompressStackNodeDto(dtoStr, strings) {
    const attrsStr = dtoStr.split('¦')
    const cDto = {
        i: parseInt(attrsStr[0]),
        a: decompressListOfInts(attrsStr[1]),
        t: parseInt(attrsStr[2]),
        l: parseInt(attrsStr[3]),
        p: (attrsStr[4]==='')?[]:attrsStr[4].split(' ').map(decompressListOfInts),
        n: parseInt(attrsStr[5]),
        r: decompressListOfInts(attrsStr[6]),
        s: decompressMapOfIntToList(attrsStr[7]),
        e: decompressListOfInts(attrsStr[8]),
    }
    return {
        id: cDto.i,
        args: cDto.a,
        type: strings[cDto.t],
        label: strings[cDto.l],
        params: cDto.p?.map(param => listOfIntsToListOfStrings(param, strings)),
        numOfTypes: cDto.n,
        retVal: listOfIntsToListOfStrings(cDto.r, strings),
        substitution: hasNoValue(cDto.s)
            ?undefined
            :Object.entries(cDto.s).reduce((acc,[k,v]) => ({...acc,[strings[k]]:listOfIntsToListOfStrings(v,strings)}), {}),
        expr: listOfIntsToListOfStrings(cDto.e, strings)
    }
}

function decompressIndexDto(cDto) {
    return {
        elems: cDto.elems.map(e => decompressIndexElemDto(e, cDto.strings.split(' ')))
    }
}

function decompressIndexElemDto(dtoStr, strings) {
    const attrsStr = dtoStr.split('¦')
    const cDto = {
        i: parseInt(attrsStr[0]),
        t: parseInt(attrsStr[1]),
        l: attrsStr[2],
        h: (attrsStr[3]==='')?[]:attrsStr[3].split(' ').map(decompressListOfInts),
        e: decompressListOfInts(attrsStr[4]),
        v: decompressMapOfInts(attrsStr[5]),
    }
    return {
        id: cDto.i,
        type: strings[cDto.t],
        label: cDto.l,
        hypotheses: cDto.h?.map(hyp => listOfIntsToListOfStrings(hyp, strings)),
        expression: listOfIntsToListOfStrings(cDto.e, strings),
        varTypes: decompressVarTypes(cDto.v,strings),
    }
}

function decompressAssertionType(assertionType) {
    return assertionType==='T'?'Theorem':assertionType==='A'?'Axiom':assertionType
}

function listOfIntsToListOfStrings(listOfInts, strings) {
    return hasNoValue(listOfInts) ? listOfInts : listOfInts.map(i => strings[i])
}

function decompressVarTypes(compressedVarTypes, strings) {
    return Object.entries(compressedVarTypes).reduce((acc,[k,v]) => ({...acc,[strings[k]]:strings[v]}), {})
}

function decompressMapOfIntToList(str) {
    if (str === '') {
        return {}
    } else {
        const keyAndValues = str.split(' ')
        const numOfPairs = Math.floor(keyAndValues.length/2)
        if (numOfPairs*2 != keyAndValues.length) {
            throw new Error('numOfPairs*2 != keyAndValues.length')
        }
        const result = {}
        for (let i = 0; i < numOfPairs; i++) {
            const idx = i*2
            result[strToInt(keyAndValues[idx])] = decompressListOfInts(keyAndValues[idx+1])
        }
        return result
    }
}

function decompressMapOfInts(str) {
    if (str === '') {
        return {}
    } else {
        const integers = decompressListOfInts(str)
        const numOfPairs = Math.floor(integers.length/2)
        if (numOfPairs*2 != integers.length) {
            throw new Error('numOfPairs*2 != integers.length')
        }
        const result = {}
        for (let i = 0; i < numOfPairs; i++) {
            const idx = i*2
            result[integers[idx]] = integers[idx+1]
        }
        return result
    }
}

const HASH_CHAR_CODE = 35
const P_CHAR_CODE = 80
function decompressListOfInts(str) {
    if (str === '') {
        return null
    } else {
        //the line below doesn't work in Safari
        // return str.split(/(?<=[#-P])/).map(strToInt)

        const result = []
        let begin = 0
        let end = 0
        while (end < str.length) {
            const charCode = str.charCodeAt(end);
            if (HASH_CHAR_CODE <= charCode && charCode <= P_CHAR_CODE) {
                result.push(strToInt(str.substring(begin,end+1)))
                begin=end+1
            }
            end++
        }
        if (begin < end) {
            result.push(strToInt(str.substring(begin,end+1)))
        }
        return result
    }
}

function strToInt(str) {
    let result = 0
    let base = 1
    for (let i = str.length-1; i >= 0; i--) {
        result += base*charToInt(str,i)
        base *= 46
    }
    return result
}

function charToInt(str, idx) {
    const charCode = str.charCodeAt(idx);
    return charCode >= 81 ? charCode - 81 : charCode - 35
}