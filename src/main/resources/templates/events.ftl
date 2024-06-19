<#import "macros/main.ftl" as main />
<#import "macros/schema.ftl" as schema />
<@main.page title="Hendelser" miljo="${miljo}" virksomhet="${virksomhet}" bruker="${bruker}">
    <section>
        <div class="flex min-h-full flex-col justify-center">
            <div class="mt-10">
                <div class="flex flex-col justify-center mb-5">
                    <h4 class="mx-auto text-xl font-bold">Hendelser i din sesjon</h4>
                </div>
                <div class="relative max-w-8xl mt-12">
                    <div class="bg-gray-900 text-white p-4 rounded-md">
                        <div class="flex justify-between items-center mb-2">
                            <span class="text-gray-400">Hendelser:</span>
                            <!--<button class="code bg-gray-800 hover:bg-gray-700 text-gray-300 px-3 py-1 rounded-md" data-clipboard-target="#code">Copy</button>-->
                        </div>
                        <div class="overflow-x-auto">
<pre id="code" class="text-gray-300">
<code id="code-view"></code>
</pre>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <script>
            const evtSource = new EventSource(window.location.pathname + "/source");
            const code = document.getElementById("code-view");

            const appendEvent = (event) => {
                const data = event.data || "";
                data.split("\\n").forEach((line) => {
                    code.innerText += line + "\n";
                });
            };
            evtSource.addEventListener("event", (event) => appendEvent(event));
            evtSource.addEventListener("error", (event) => appendEvent(event));
        </script>
    </section>
</@main.page>

