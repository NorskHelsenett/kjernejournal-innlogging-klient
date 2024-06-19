<#import "macros/main.ftl" as main />
<#import "macros/schema.ftl" as schema />
<@main.page title="Feil" events="true">
    <section>
        <div class="flex min-h-full flex-col justify-center">
            <div class="mt-10">
                <div class="flex flex-col justify-center mb-5">
                    <h4 class="mx-auto text-2xl font-black">Feil i ${kilde}</h4>
                    <h1 class="mx-auto text-8xl font-black">${status}</h1>
                    <h4 class="mx-auto text-xl font-black">${kontekst}</h4>
                </div>
                <p class="mx-auto text-l font-semibold">${melding}</p>
                <a href="/"
                   class="mt-8 flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f]
               focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]">Gå
                    tilbake</a>
                <#if stacktrace?has_content>
                    <div class="relative max-w-4xl mt-12">
                        <div class="bg-gray-900 text-white p-4 rounded-md">
                            <div class="flex justify-between items-center mb-2">
                                <span class="text-gray-400">Stacktrace:</span>
                                <!--<button class="code bg-gray-800 hover:bg-gray-700 text-gray-300 px-3 py-1 rounded-md" data-clipboard-target="#code">Copy</button>-->
                            </div>
                            <div class="overflow-x-auto">
<pre id="code" class="text-gray-300">
<code>${stacktrace}</code>
</pre>
                            </div>
                        </div>
                    </div>
                </#if>
            </div>
        </div>
    </section>
</@main.page>

