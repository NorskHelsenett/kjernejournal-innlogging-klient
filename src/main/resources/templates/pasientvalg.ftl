<#import "macros/main.ftl" as main />
<#import "macros/schema.ftl" as schema />
<@main.page title="Pasientvalg - EPJ" miljo="${miljo}" virksomhet="${virksomhet}" bruker="${bruker}" autorisasjon="${autorisasjon}" actions=[{"text": "Logg ut", "href":"/pasientvalg/loggut"}] events="true">
    <section>
        <div class="flex min-h-full flex-col justify-center">
            <div class="mt-10">
                <form id="pasientvalg" class="space-y-6" action="/pasientvalg/velg" method="POST">
                    <div class="grid gap-6 mb-6 md:grid-cols-2">
                        <div>
                            <label for="pasient" class="block mb-2 text-sm font-medium leading-6 text-gray-900">
                                Pasient
                            </label>
                            <input id="pasient" type="text" name="pasient" pattern="[0-9]{11}"
                                   oninvalid="this.setCustomValidity('Skal være et tall med 11 sifre, og det er obligatorisk.')"
                                   oninput="this.setCustomValidity('')"
                                   placeholder="Pasientens fødselsnummer" required
                                   class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                   focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                   block w-full p-2.5 placeholder-gray-400"/>
                        </div>
                        <div>
                            <label for="access_basis"
                                   class="block mb-2 text-sm font-medium leading-6 text-gray-900">
                                Grunnlag
                            </label>
                            <select id="access_basis" name="access_basis" required
                                    class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                block w-full p-2.5">
                                <option id="AKUTT" value="AKUTT">Akutt</option>
                                <option id="SAMTYKKE" value="SAMTYKKE">Samtykke</option>
                                <option id="UNNTAK" value="UNNTAK">Unntak</option>
                            </select>
                        </div>
                    </div>
                    <div class="flex flex-col">
                        <label for="attest"
                               class="block mb-2 text-sm font-medium leading-6 text-gray-900">Attest:</label>
                        <textarea id="attest" name="attest" cols="50" rows="20" required
                                  class="block p-2.5 w-full text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]">${attest}
                            </textarea>
                    </div>
                    <div class="flex w-full justify-center py-4">
                        <button class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]"
                                type="submit" onclick="lagreSkjema('pasientvalg', ['attest'])">Neste
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </section>
    <@schema.storageScript />
    <script>
        hentSkjema("pasientvalg");
    </script>
</@main.page>