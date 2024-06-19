<#import "macros/main.ftl" as main />
<#import "macros/schema.ftl" as schema />
<@main.page title="Innlogging - EPJ">
    <section>
        <div class="flex min-h-full flex-col justify-center">
            <div class="mt-10">
                <form class="space-y-6" id="innlogging" action="/api/login" method="POST">
                    <#if endringer?has_content >
                        <div id="${endringsid}" style="white-space: normal;"
                             class="justify-left rounded-md bg-[#c4f2da] px-3 py-1.5 text-sm font-bold leading-6 text-black shadow-sm">
                            <div>
                                Siste endringer
                                <button style="float: right;" type="button"
                                        onclick='lukkBanner("${endringsid}")'>&times;
                                </button>
                                <br>
                                <ul style="list-style: disc inside;"><#list endringer as endring>
                                        <li>${endring.value}</li></#list></ul>
                                <br>
                                Ta kontakt i <a href="https://norskhelsenett.slack.com/archives/C01CX3G8XMM"><u>NHNs
                                        Slack-kanal</u></a> om du opplever problemer.
                            </div>

                        </div>
                    </#if>
                    <div class="grid gap-6 mb-6 md:grid-cols-2">
                        <div>
                            <label for="bruker"
                                   class="block mb-2 text-sm font-medium leading-6 text-gray-900">Bruker</label>
                            <input id="bruker"
                                   name="bruker"
                                   type="text"
                                   pattern="[0-9]{11}"
                                   placeholder="Brukers fødseslnummer"
                                   class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                   focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                   block w-full p-2.5 placeholder-gray-400">
                        </div>
                        <div>
                            <label for="autorisasjon"
                                   class="block mb-2 text-sm font-medium leading-6 text-gray-900">Autorisasjon</label>
                            <select id="autorisasjon" name="autorisasjon" required
                                    class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                block w-full p-2.5">
                                <#list autorisasjoner?sort_by("label") as autorisasjon>
                                    <option value="${autorisasjon.value}" label="${autorisasjon.label}"/>
                                </#list>
                            </select>
                        </div>
                    </div>
                    <div class="grid gap-6 mb-6 md:grid-cols-2">
                        <div>
                            <label for="klient"
                                   class="block mb-2 text-sm font-medium leading-6 text-gray-900">Virksomhet</label>
                            <select id="klient" name="klient" required
                                    class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                block w-full p-2.5">
                                <#list klienter?sort_by("label") as klient>
                                    <option value="${klient.value}" label="${klient.label}"/>
                                </#list>
                            </select>
                        </div>
                        <div>
                            <label for="miljo" class="block mb-2 text-sm font-medium leading-6 text-gray-900">
                                Miljø
                            </label>
                            <select id="miljo" name="miljo" required
                                    class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg
                                focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]
                                block w-full p-2.5">
                                <#list miljo?sort_by("label") as m>
                                    <option value="${m.value}" label="${m.label}"/>
                                </#list>
                            </select>
                        </div>
                    </div>
                    <div class="flex w-full justify-center">
                        <button type="submit" onclick="lagreSkjema('innlogging')"
                                class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]"
                        >
                            Logg inn med HelseID
                            <span class="material-symbols-outlined icon">login</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </section>
    <@schema.sjekkBannerScript />
    <@schema.storageScript />
    <script>
        hentSkjema("innlogging");
    </script>
</@main.page>
