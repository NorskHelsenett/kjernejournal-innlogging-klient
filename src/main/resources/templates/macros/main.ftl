<#macro page title virksomhet="" bruker="" autorisasjon="" events="" pasient="" grunnlag="" actions=[] miljo="">
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>${title}</title>
        <link rel="icon" type="image/x-icon" href="/assets/faviconV2.png">
        <script src="/assets/tailwind.js"></script>
        <link rel="stylesheet" href="/assets/material-symbols.css"/>
    </head>
    <body class="h-full">
    <header class="min-h-full">
        <nav class="bg-gray-100">
            <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <div class="flex h-16 items-center justify-between">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <img class="h-14 w-auto" src="/assets/logo.png"
                                 alt="Kjernejournal EPJ">
                        </div>
                    </div>
                    <div class="flex items-center justify-between gap-2">
                        <#if miljo?has_content>
                            <div
                                    class="flex justify-center rounded-md bg-[#585858] px-3 py-1.5 text-sm font-semibold text-white shadow-sm">
                                Kjernejournal-miljø: ${miljo}</div>
                        </#if>
                        <#list actions as a>
                            <a href="${a.href}"
                               class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]">
                                ${a.text}</a>
                        </#list>
                        <#if events?has_content>
                            <a href="/events" target="_blank"
                               class="flex justify-center rounded-md bg-[#015945] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#41bc9f] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#015945]">
                                Hendelser</a>
                        </#if>
                    </div>
                </div>
            </div>
        </nav>
    </header>
    <main class="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
        <div class="bg-white flex w-full justify-stretch">
            <#if virksomhet?has_content>
                <div class="mt-2 px-4 py-2 flex-1 rounded-md bg-[#dadada] text-bold grid grid-cols-[max-content_1fr] gap-x-4">
                    <#if virksomhet?has_content>
                        <div>Virksomhet:</div>
                        <div>${virksomhet}</div>
                    </#if>
                    <#if bruker?has_content>
                        <div>Bruker:</div>
                        <div>${bruker}</div>
                    </#if>
                    <#if autorisasjon?has_content>
                        <div>Autorisasjon:</div>
                        <div>${autorisasjon}</div>
                    </#if>
                    <#if pasient?has_content>
                        <div>Pasient:</div>
                        <div>${pasient}</div>
                    </#if>
                    <#if grunnlag?has_content>
                        <div>Grunnlag:</div>
                        <div>${grunnlag}</div>
                    </#if>
                </div>
            </#if>
        </div>
        <#-- This processes the enclosed content:  -->
        <#nested>
    </main>
    </body>
    </html>
</#macro>

