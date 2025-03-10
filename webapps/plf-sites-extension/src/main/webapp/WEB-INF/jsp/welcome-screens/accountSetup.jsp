<%--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

--%>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService" %>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="org.gatein.portal.controller.resource.ResourceRequestHandler" %>
<%@ page import="org.exoplatform.portal.resource.SkinConfig" %>
<%@ page import="org.exoplatform.portal.branding.Utils"%>
<%@ page language="java" %>
<%
    String usernameRegExp = System.getProperty("gatein.validators.username.regexp");
    if(usernameRegExp==null) usernameRegExp="";
    String formatMsg = System.getProperty("gatein.validators.username.format.message");
    String usernameMinLength = System.getProperty("gatein.validators.username.length.min");
    String usernameMaxLength = System.getProperty("gatein.validators.username.length.max");
    int max=0;
    int min=0;
    if(usernameMaxLength!=null) {
        max = Integer.parseInt(usernameMaxLength);
    }
    if(usernameMinLength!=null) {
        min = Integer.parseInt(usernameMinLength);
    }
    String contextPath = request.getContextPath() ;
    String lang = request.getLocale().getLanguage();
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html; charset=UTF-8");

  PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
  ResourceBundleService service = (ResourceBundleService) portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
  ResourceBundle rb = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale());

  UserPortalConfigService userPortalConfigService = portalContainer.getComponentInstanceOfType(UserPortalConfigService.class);
  String skinName = userPortalConfigService.getMetaPortalSkinName();
  SkinService skinService = portalContainer.getComponentInstanceOfType(SkinService.class);
  SkinConfig skin = skinService.getSkin("portal/AccountSetup", skinName);
  String cssPath = "";
  if(skin != null) {
    cssPath = skin.getCSSPath()+"?v="+ResourceRequestHandler.VERSION;
  } else {
    cssPath = skinService.getSkin("portal/AccountSetup", "Enterprise").getCSSPath()+"?v="+ResourceRequestHandler.VERSION;
  }
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="<%=lang%>" lang="<%=lang%>">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <link rel="shortcut icon" type="image/x-icon"  href="<%= Utils.getFaviconPath() %>" />
    <link id="brandingSkin" rel="stylesheet" type="text/css" href="/rest/v1/platform/branding/css">
    <link href="/social/skin/css/welcome-screens/jquery.qtip.min.css" rel="stylesheet" type="text/css" />
    <link href="<%=cssPath%>" rel="stylesheet" type="text/css" />

    <script type="text/javascript" src="/social/js/lib/jquery-3.2.1.js"></script>
    <script type="text/javascript" src="/social/js/welcome-screens/accountSetup.js"></script>

</head>
<div class="backLight"></div>
<div class="uiWelcomeBox" id="AccountSetup1"  >
    <div class="header"><%=rb.getString("AccountSetupViewServlet.label.header1")%></div>
    <div class="content form-horizontal" id="AccountSetup">
        <h5><%=rb.getString("AccountSetupViewServlet.label.create_account")%></h5>
        <p class="desc"><%=rb.getString("AccountSetupViewServlet.label.primary_user")%></p>
        <form name="tcForm" action="<%= contextPath + "/accountSetupAction"%>" method="post">
            <div class="control-group" id ="usernameId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.userName")%>:</label>
                <div class="controls">
                    <input type="text" name="username" id="userNameAccount" placeholder="<%=rb.getString("AccountSetupViewServlet.label.user_Name")%>" class="inputFieldLarge"/>
                    <input type="hidden" id="usernameRegExpid" value="<%=usernameRegExp%>"/>
                    <input type="hidden" id="formatMsgid" value="<%=formatMsg%>"/>
                    <input type="hidden" id="usernameMaxLengthid" value="<%=max%>"/>
                    <input type="hidden" id="usernameMinLengthid" value="<%=min%>"/>
                </div>
            </div>
            <div class="control-group" id="fullnameId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.full_Name")%>:</label>
                <div class="controls"><input type="text" name="firstNameAccount" id="firstNameAccount" placeholder="<%=rb.getString("AccountSetupViewServlet.label.first_Name")%>" class="inputFieldMedium"/><input type="text" name="lastNameAccount" id="lastNameAccount" placeholder="<%=rb.getString("AccountSetupViewServlet.label.last_Name")%>" class="inputFieldMedium" /></div>
            </div>
            <div class="control-group" id="emailId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.email")%>:</label>
                <div class="controls"><input type="text" name="emailAccount" id="emailAccount" class="inputFieldLarge" /></div>
            </div>
            <div class="control-group" id="passwordId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.password")%>:</label>
                <div class="controls"><input type="password" name="password" id="userPasswordAccount" class="inputFieldMini" /><span class="confirmLabel"><%=rb.getString("AccountSetupViewServlet.label.confirm")%>:</span><input type="password" name="confirmUserPasswordAccount" id="confirmUserPasswordAccount" class="inputFieldMini" />
                </div>
            </div>

            <h5><%=rb.getString("AccountSetupViewServlet.label.addmin_password")%></h5>
            <p class="desc"><%=rb.getString("AccountSetupViewServlet.label.login_root")%></p>
            <div class="control-group" id="adminUsernameId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.userName")%>:</label>
                <div class="controls"><input type="text" name="adminFirstName" id="adminFirstName" placeholder="root" readonly="readonly" class="inputFieldLarge disable" /></div>
            </div>
            <div class="control-group" id="adminPasswordId">
                <label class="control-label"><%=rb.getString("AccountSetupViewServlet.label.password")%>:</label>
                <div class="controls">
                    <input type="password" name="adminPassword" id="adminPassword" class="inputFieldMini" /><span class="confirmLabel"><%=rb.getString("AccountSetupViewServlet.label.confirm")%>:</span><input type="password" name="confirmAdminPassword" id="confirmAdminPassword" class="inputFieldMini" />
                </div>
            </div>
    </div>
    <!-- Please do not make it Button it may cause blocker problem -->
    <div class="bottom">
        <button class="btn btn-primary" id="continueButton" onclick="WelcomeScreens.exit();return false;"><%=rb.getString("AccountSetupViewServlet.label.submit")%></button>
        <button class="btn" name="setupbutton" value="skipform"><%=rb.getString("AccountSetupViewServlet.label.skip")%></button>
    </div>
</div>
</div>
<div>
    <!--	<div class="backLight"></div>    -->
    <div class="uiWelcomeBox" id="Greetings" style="display: none">
        <div class="header"><%=rb.getString("AccountSetupViewServlet.label.greetings")%></div>
        <div class="content form-horizontal" id="AccountSetup">
            <p>
                <strong><%=rb.getString("AccountSetupViewServlet.label.almost_done")%></strong>, <%=rb.getString("AccountSetupViewServlet.label.add_your_colleagues")%>.
            </p>
        </div>
        <div class="bottom"><button name="setupbutton" value="submitform" class="btn btn-primary"><%=rb.getString("AccountSetupViewServlet.label.start")%></button></div>
    </div>
</div>
</form>
</html>
