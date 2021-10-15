"use strict";

function MetamathIndexView({elems}) {

    useEffect(() => {
        document.title = `Index - Metamath Proof Explorer`
    }, [])

    const {openLicenseDialog, renderLicenseDialog} = useLicenseDialog()

    const s = {
        STATE_NUMBER: 'STATE_NUMBER',
        PAGE_NUMBER: 'PAGE_NUMBER',
        NUMBER_OF_PAGES: 'NUMBER_OF_PAGES',
        POSSIBLE_TYPES: 'POSSIBLE_TYPES',
        TYPE_FILTER: 'TYPE_FILTER',
        LABEL_FILTER: 'LABEL_FILTER',
        SYMBOL_FILTER: 'SYMBOL_FILTER',
        IDS_TO_SHOW: 'IDS_TO_SHOW',
        FILTERED_ELEMS: 'FILTERED_ELEMS',
        MATCHED_INDEXES: 'MATCHED_INDEXES',
    }
    const itemsPerPage = 20

    const [state, setState] = useState(() => createNewState({}))
    const labelFilterRef = useRef(null)
    const symbolsFilterRef = useRef(null)

    function createNewState({prevState, params}) {

        function getParamValue(paramName, defaulValue) {
            return params?.[paramName]??prevState?.[paramName]??defaulValue
        }

        const stateNumber = getParamValue(s.STATE_NUMBER,0)

        const typeFilter = getParamValue(s.TYPE_FILTER,'')
        const labelFilter = getParamValue(s.LABEL_FILTER,'').trim().toLowerCase()
        const symbolFilter = getParamValue(s.SYMBOL_FILTER,'').trim()
        const symbolsToSearch = symbolFilter.split(/\s+/).filter(s => s.length)

        const filteredElems = elems.filter(elem => {
            if (symbolsToSearch.length) {
                elem.matchedIndexes = assertionMatchSymbols({assertion: elem, symbols: symbolsToSearch})
            } else {
                elem.matchedIndexes = null
            }
            return (!labelFilter || elem.label.toLowerCase().indexOf(labelFilter) >= 0)
                && (symbolsToSearch.length == 0 || elem.matchedIndexes.find(ii => ii.length == symbolsToSearch.length))
                && (typeFilter == '' || elem.type == typeFilter)
        })
        const numOfPages = Math.ceil(filteredElems.length/itemsPerPage)

        const pageNumber = Math.max(
            1,
            Math.min(
                numOfPages,
                (prevState?.[s.TYPE_FILTER]!=typeFilter
                    || prevState?.[s.LABEL_FILTER]!=labelFilter
                    || prevState?.[s.SYMBOL_FILTER]!=symbolFilter)
                    ? 1
                    : getParamValue(s.PAGE_NUMBER,1)
            )
        )

        const minIdx = itemsPerPage*(pageNumber-1)
        const maxIdx = itemsPerPage*pageNumber-1
        const idsToShow = filteredElems.filter((e,i) => minIdx <= i && i <= maxIdx).map(e => e.id)

        return createObj({
            [s.STATE_NUMBER]: stateNumber+1,
            [s.PAGE_NUMBER]: pageNumber,
            [s.NUMBER_OF_PAGES]: numOfPages,
            [s.POSSIBLE_TYPES]: ['', ...new Set(elems.map(e => e.type))],
            [s.TYPE_FILTER]: typeFilter,
            [s.LABEL_FILTER]: labelFilter,
            [s.SYMBOL_FILTER]: symbolFilter,
            [s.IDS_TO_SHOW]: idsToShow,
            [s.FILTERED_ELEMS]: filteredElems,
        })
    }

    function applyFilters({typeFilter}) {
        setState(prevState => createNewState({
            prevState,
            params:{
                [hasValue(typeFilter)?s.TYPE_FILTER:undefined]:typeFilter,
                [s.LABEL_FILTER]:labelFilterRef.current.value,
                [s.SYMBOL_FILTER]:symbolsFilterRef.current.value,
            }
        }))
    }

    function exprMatchSymbols({expr,symbols}) {
        const result = []
        let si = 0
        for (let ei = 0; ei < expr.length && si < symbols.length; ei++) {
            if (expr[ei] === symbols[si]) {
                si++
                result.push(ei)
            }
        }
        return result.length == symbols.length ? result : []
    }

    function assertionMatchSymbols({assertion,symbols}) {
        const result = []
        if (assertion.hypotheses?.length) {
            for (let i = 0; i < assertion.hypotheses.length; i++) {
                result.push(exprMatchSymbols({expr:assertion.hypotheses[i],symbols}))
            }
        }
        result.push(exprMatchSymbols({expr:assertion.expression,symbols}))
        return result
    }

    function renderTable() {
        const idsToShow = state[s.IDS_TO_SHOW]
        return re(MetamathIndexTable, {
            idsToShow,
            allElems:state[s.FILTERED_ELEMS],
            stateNumber:state[s.STATE_NUMBER],
        })
    }

    function renderPagination() {
        return state[s.NUMBER_OF_PAGES]>1?re(Pagination,{
            numOfPages:state[s.NUMBER_OF_PAGES],
            curPage:state[s.PAGE_NUMBER],
            onChange: newPageNum => setState(prevState => createNewState({prevState, params:{[s.PAGE_NUMBER]:newPageNum}}))
        }):null
    }

    function renderControls() {
        return RE.Container.row.left.center({},{style: {marginRight:'10px'}},
            RE.FormControl({variant:'outlined',size: 'small'},
                RE.InputLabel({}, 'Type'),
                RE.Select(
                    {
                        label:'Type',
                        onChange: event => {
                            const newTypeFilter = event.target.value
                            if (state[s.TYPE_FILTER] !== newTypeFilter) {
                                applyFilters({typeFilter:newTypeFilter})
                            }
                        },
                        style: {width: 150}
                    },
                    state[s.POSSIBLE_TYPES].map(t =>
                        RE.MenuItem({key: 'assertionType'+t, value: t}, t!==''?decompressAssertionType(t):'All')
                    )
                )
            ),
            RE.TextField(
                {
                    inputRef:labelFilterRef,
                    variant: 'outlined', label: 'Label',
                    style: {width: 300},
                    size: 'small',
                    onKeyDown: event => event.nativeEvent.keyCode == 13
                        ? applyFilters({})
                        : null,
                }
            ),
            RE.TextField(
                {
                    inputRef:symbolsFilterRef,
                    variant: 'outlined', label: 'Symbols',
                    style: {width: 300},
                    size: 'small',
                    onKeyDown: event => event.nativeEvent.keyCode == 13
                        ? applyFilters({})
                        : null,
                }
            ),
            renderPagination()
        )
    }

    return RE.Container.col.top.left({},{style: {marginBottom:'10px'}},
        renderControls(),
        renderTable(),
        RE.div({style:{marginTop:'20px', fontSize:'11px', face:"sans-serif"}},
            "Copyright terms: ",
            RE.a({onClick:openLicenseDialog, href:'#'},"Public domain")
        ),
        renderLicenseDialog()
    )
}
