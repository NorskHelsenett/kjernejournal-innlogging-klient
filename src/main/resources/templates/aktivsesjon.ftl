<#import "macros/main.ftl" as main />
<#import "macros/schema.ftl" as schema />
<@main.page title="Kjernejournal EPJ" miljo="${miljo}" virksomhet="${virksomhet}" bruker="${bruker}" autorisasjon="${autorisasjon}" pasient="${pasient}" grunnlag="${grunnlag}"
actions=[{"text": "Logg ut", "href":"/sesjon/loggut"}] events="true">
    <section>
            <div class="flex min-h-full flex-col justify-center">
                <div class="mt-10">
                    <div class="grid gap-6 mb-6 md:grid-cols-3">
                        <div>
                            <a href="sesjon/byttpasient" target="_blank"
                               class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f]
               focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]"><span
                                        class="material-symbols-outlined">arrow_back</span> Bytt pasient
                            </a>
                        </div>
                        <div>
                            <a id="aapneKj" href="${kjernejournalUrl}" target="_blank"
                               class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f]
               focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]"
                            >Åpne Kjernejournal i ${miljo}</a>
                        </div>
                        <div>
                            <img src="/assets/indikator/${indikator}.png"  class="object-cover h-12 w-12">
                        </div>
                    </div>
                </div>
            </div>
    </section>
    <@schema.funksjonalitetScript />
    <script type="application/javascript">

        const holdSesjon = async () => {
            const response = await fetch("/sesjon/holdsesjon", {method: "POST"});
            if (response.status < 300) {
                const data = await response.json();
                return data && data.levetid !== undefined ? data.levetid : -1;
            } else {
                console.error("Klarte ikke holde sesjon: ", response.text());
                return -1;
            }
        }

        const timeout = (seconds) => {
            return new Promise(resolve => setTimeout(resolve, seconds * 1000));
        }

        const holdSesjonILive = async () => {
            let ventISekund = 30
            while (true) {
                await timeout(ventISekund)
                const nyVentetid = await holdSesjon()
                if (nyVentetid > 0) {
                    ventISekund = nyVentetid
                } else {
                    break; // Refresh feilet, avslutt
                }
            }
        }

        holdSesjonILive()
    </script>
</@main.page>
