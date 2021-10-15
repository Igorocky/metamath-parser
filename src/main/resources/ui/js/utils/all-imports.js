'use strict';

function createHeadChildElem({tagName,onLoad,attrs}) {
    const elem = document.createElement(tagName)
    elem.onload=onLoad
    for (let attr in attrs) {
        elem[attr]=attrs[attr]
    }
    document.getElementsByTagName('head')[0].appendChild(elem)
}

function includeStyle({path, onLoad}) {
    createHeadChildElem({tagName:'link',onLoad,attrs:{href:path,rel:'stylesheet'}})
}

function includeScript({path, onLoad}) {
    createHeadChildElem({tagName:'script',onLoad,attrs:{src:path}})
}

function includeScripts({pathPrefix, scripts, onLoad}) {
    scripts.reduceRight(
        (acc,path) => () => includeScript({path:`${pathPrefix}/${path}`, onLoad:acc}),
        () => onLoad?.()
    )()
}

const dirWithScripts = `${relPathToRoot}/${version}`

createHeadChildElem({
    tagName:'meta',
    attrs:{name:'viewport',content:'minimum-scale=1, initial-scale=1, width=device-width, shrink-to-fit=no'}
})

createHeadChildElem({
    tagName:'link',
    attrs:{href:dirWithScripts+'/img/favicon.ico',rel:'shortcut icon',type:'image/x-icon'}
})

includeStyle({
    path: dirWithScripts + '/css/styles.css',
    onLoad: () => includeScripts({
        pathPrefix: dirWithScripts,
        scripts: [
            'js/lib/react.production-16.8.6.min.js',
            'js/lib/react-dom.production-16.8.6.min.js',
            'js/lib/material-ui.production-4.11.0.min.js',
            'js/utils/react-imports.js',
            'js/utils/data-functions.js',
            'js/utils/svg-functions.js',
            'js/utils/rendering-functions.js',
            'js/components/LicenseDialogView.js',
            'js/components/Pagination.js',
            'js/components/Expression.js',
            'js/components/Assertion.js',
            'js/components/ConstProofNode.js',
            'js/components/RuleProofNode.js',
            'js/components/MetamathAssertionView.js',
            'js/components/MetamathIndexTable.js',
            'js/components/MetamathIndexView.js',
        ],
        onLoad: () => {
            const decompressedViewProps = window[decompressionFunction](viewProps)
            window['decompressedViewProps'] = decompressedViewProps
            ReactDOM.render(
                re(window[viewComponent], decompressedViewProps),
                document.getElementById('react-container')
            )
        }
    })
})