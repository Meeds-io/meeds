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
<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="org.exoplatform.services.organization.User"%>
<%@ page import="java.util.Collection" %>
<%@ page import="org.exoplatform.portal.resource.SkinConfig" %>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService" %>
<%@ page import="org.exoplatform.services.resources.LocaleConfigService"%>
<%@ page import="org.exoplatform.services.resources.LocaleConfig"%>
<%@ page import="org.exoplatform.services.resources.Orientation"%>
<%@ page import="org.exoplatform.portal.branding.Utils"%>
<%@ page language="java" %>
<%
    PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
    ResourceBundleService service = portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale()) ;
    String contextPath = portalContainer.getPortalContext().getContextPath();

    UserPortalConfigService userPortalConfigService = portalContainer.getComponentInstanceOfType(UserPortalConfigService.class);
    SkinService skinService = portalContainer.getComponentInstanceOfType(SkinService.class);
    String skinName = userPortalConfigService.getMetaPortalSkinName();
    Collection<SkinConfig> skins = skinService.getPortalSkins(skinName);
    String loginCssPath = skinService.getSkin("portal/login", skinName).getCSSPath();

    User portalUser = (User)request.getAttribute("portalUser");
    User detectedUser = (User)request.getAttribute("detectedUser");
    String detected = detectedUser.getUserName();
    if (!detected.equals(portalUser.getUserName()) && detectedUser.getEmail().equals(portalUser.getEmail())) {
        detected = detectedUser.getEmail();
    }

    String error = (String)request.getAttribute("invitationConfirmError");

    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html; charset=UTF-8");

    String browserLanguage = request.getLocale() == null ? "en" : request.getLocale().getLanguage();
    LocaleConfigService localeConfigService = portalContainer.getComponentInstanceOfType(LocaleConfigService.class);
    LocaleConfig localeConfig = localeConfigService.getLocaleConfig(browserLanguage);
    String direction = localeConfig == null || localeConfig.getOrientation() != Orientation.RT ? "ltr" : "rtl";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="<%=browserLanguage%>" lang="<%=browserLanguage%>" dir="<%=direction%>">
    <head>
        <title>Oauth invitation</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>		
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link id="brandingSkin" rel="stylesheet" type="text/css" href="/rest/v1/platform/branding/css">
        <link rel="shortcut icon" type="image/x-icon"  href="<%= Utils.getFaviconPath() %>" />
        <% for (SkinConfig skin : skins) {
            if ("Core".equals(skin.getModule())) {%>
                <link href="<%=skin.getCSSPath()%>" rel="stylesheet" type="text/css" test="<%=skin.getModule()%>"/>
            <%}%>
        <%}%>
        <link href="<%=loginCssPath%>" rel="stylesheet" type="text/css"/>
        <script type="text/javascript" src="/social/js/lib/jquery-3.2.1.js"></script>
        <script type="text/javascript" src="/social/js/lib/deprecated/switch-button.js"></script>
    </head>
    <body class="modal-open">
	<div class="uiPopupWrapper">
        <div class="UIPopupWindow modal uiOauthInvitation uiPopup UIDragObject NormalStyle" style="width: 430px; margin-left: -215px; border-radius: 4px">
          <div class="popupHeader ClearFix">
              <a href="<%= contextPath + "/login?login_controller=oauth_cancel"%>" class="uiIconClose pull-right" aria-hidden="true" ></a>
              <span class="PopupTitle popupTitle"><%= res.getString("UIOAuthInvitationForm.title") %></span>
          </div>
          <div class="PopupContent popupContent">
            <form name="registerForm" action="<%= contextPath + "/login"%>" method="post" style="margin: 0px;">
            <div class="content mgT5">
                    <p><%=res.getString("UIOAuthInvitationForm.message.detectedUser")%><br/><strong><%=detected %></strong></p>
                    <p><%=res.getString("UIOAuthInvitationForm.message.inviteMessage")%></p>
                    <div class="clearfix">
                        <label class="pull-left"><%=res.getString("UIOAuthInvitationForm.label.password")%></label>
                        <div class="pull-right password-field">
                            <input class="password mg0-ipt <%=(error != null ? "error" : "")%>" type="password" name="password" placeholder="<%=res.getString("portal.login.Password")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Password")%>'" onfocus="this.placeholder = ''"/>
                            <div class="parentPosition" style="display:inline-block;" onmouseout="(function(elm) {$(elm).find('.popupOverContent:first').hide();})(this)" onmouseover="(function(elm) {$(elm).find('.popupOverContent:first').show();})(this)">
                                <i class="uiIconQuestion uiIconLightGray"></i>
                                <div class="gotPosition" style="position: relative; display:block">
                                    <div class="popover left popupOverContent" style="width: 230px; left: -232px; top: -73px; display: none;">
                                        <span class="arrow" style="top: 52%;"></span>
                                        <div class="popover-content"><%=res.getString("UIOAuthInvitationForm.tooltip.message")%></div>
                                    </div>
                                </div>
                            </div>
                            <% if (error != null) { %>
                            <br>
                            <span class="mgT5" style="display: inline-block"><i class="uiIconColorError"></i> <%=error%></span>
                            <%}%>
                        </div>
                        <input type="hidden" name="login_controller" value="confirm_account"/>
                    </div>
                </div>
                <div class="uiAction uiActionBorder">
                    <button type="submit" class="btn btn-primary"><%=res.getString("portal.login.Confirm")%></button>
                    <a class="btn ActionButton LightBlueStyle" href="<%= contextPath + "/login?login_controller=register"%>"><%=res.getString("UIOAuthInvitationForm.action.NewAccount")%></a>
                </div>
            </form>
        </div>
    </div>
    </div>
    </body>
</html>
