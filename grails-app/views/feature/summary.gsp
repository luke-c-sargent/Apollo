<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-feature" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                              default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
    %{--<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>--}%
    %{--<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>--}%
</ul>
</div>

<div id="show-feature" class="content scaffold-show" role="main">
    <h3>Summary</h3>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <g:render template="summaryEntry" model="[summaryData: featureSummaryInstance]"/>
    <g:each in="${featureSummaries}" var="featureSummaryInstance">
        <g:render template="summaryEntry"
                  model="[organism: featureSummaryInstance.key, summaryData: featureSummaryInstance.value]"/>
    </g:each>
</div>
</body>
</html>
