<#macro storageScript>
    <script>
        function lagreSkjema(formId, ignoreNameList) {
            const formData = new FormData(document.getElementById(formId));
            for (let pair of formData.entries()) {
                const name = pair[0];
                if ((ignoreNameList || []).includes(name)) {
                    continue;
                }
                const value = pair[1];
                window.localStorage.setItem(name, value);
            }
        }

        function hentSkjema(formId) {
            const formData = new FormData(document.getElementById(formId));
            for (let pair of formData.entries()) {
                const value = window.localStorage.getItem(pair[0])

                if (value) {
                    document.getElementsByName(pair[0])[0].value = value;
                }
            }
        }
    </script>
</#macro>
<#macro funksjonalitetScript>
    <style>
    .disabled-link {
      pointer-events: none;
    }
    </style>
    <script>
        const deaktiverLinkKlass = "flex justify-center rounded-md bg-gray-100 px-3 py-1.5 text-sm font-semibold leading-6 shadow-sm text-gray-400 disabled-link";
        const aapenKnap = document.getElementById("aapneKj");
        aapenKnap.onclick = (() => {
            aapenKnap.className = deaktiverLinkKlass;
        });
    </script>
</#macro>
<#macro sjekkBannerScript >
    <script>
        lukkBanner();

        function lukkBanner(id) {
            if (id != null) {
                localStorage.setItem("lukketBannerId", id)
            }
            const element = document.getElementById(localStorage.getItem("lukketBannerId"))
            if (element) element.remove()
        }
    </script>
</#macro>
