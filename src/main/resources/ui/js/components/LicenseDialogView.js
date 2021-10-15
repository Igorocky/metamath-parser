"use strict";

function useLicenseDialog() {
    const [dialogOpened, setDialogOpened] = useState(false)

    function renderLicenseDialog() {
        return dialogOpened?RE.Dialog({open:true, onClose:() => setDialogOpened(false)},
            RE.DialogTitle({},"Copyright terms: Public domain"),
            RE.DialogContent({},
                RE.Typography({variant:'h6'},
                    'Copyright terms'
                ),
                RE.DialogContentText({},
                    'This web page is placed in the public domain per the ',
                    RE.a({href:'https://creativecommons.org/publicdomain/zero/1.0/'},'CC0 Public Domain Dedication'),
                    ' [accessed 19-Dec-2020].'
                ),
                RE.DialogContentText({},
                    `The public domain release applies to the original content on this page, 
                    which (to the author's best knowledge) is all text and images displayed on the page 
                    other than any short attributed quotations from copyrighted sources.`
                ),
                RE.DialogContentText({},
                    `The public domain release applies worldwide. In case this is not legally possible, the right is granted to use the work for any purpose, without any conditions, unless such conditions are required by law.`
                ),
                RE.Typography({variant:'h6'},
                    'Page content'
                ),
                RE.DialogContentText({},
                    'This web page represents visualization of data taken from ',
                    RE.a({href:'http://metamath.org'},'metamath.org'),
                ),
                RE.Typography({variant:'h6'},
                    'Metamath Logo'
                ),
                RE.DialogContentText({},
                    'The icon used by this web page is a modified version of  ',
                    RE.a({href:'http://us.metamath.org/copyright.html#logo'},'Metamath Logo'),
                )
            ),
            RE.DialogActions({},
                RE.Button({variant:'contained', onClick:() => setDialogOpened(false)},"Close")
            ),
        ):null
    }

    return {
        openLicenseDialog: () => setDialogOpened(true),
        renderLicenseDialog
    }
}
