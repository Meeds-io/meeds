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
<%@ page import="org.exoplatform.services.organization.impl.UserImpl" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.exoplatform.portal.resource.SkinConfig" %>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService" %>
<%@ page import="java.util.Collection" %>
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

    User user = (User)request.getAttribute("portalUser");
    if (user == null) {
        user = new UserImpl();
    }

    Boolean isOnFlyError = (Boolean)request.getAttribute("isOnFlyError");

    List<String> errors = (List<String>)request.getAttribute("register_errors");
    Set<String> errorFields = (Set<String>)request.getAttribute("register_error_fields");
    if (errors == null) {
        errors = new ArrayList<String>();
    }

    if (isOnFlyError != null && isOnFlyError) {
        errors.add(0, res.getString("UIRegisterForm.message.signUpOnFlyError"));
    }

    if (errorFields == null) {
        errorFields = new HashSet<String>();
    }
    String errorClass = "error";

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
        <title>Oauth register</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>		
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="shortcut icon" type="image/x-icon"  href="<%= Utils.getFaviconPath() %>" />
        <link id="brandingSkin" rel="stylesheet" type="text/css" href="/rest/v1/platform/branding/css">
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
        <div class="UIPopupWindow uiPopup modal uiOauthRegister UIDragObject NormalStyle">
          <div class="popupHeader ClearFix">
              <a href="<%= contextPath + "/login?login_controller=oauth_cancel"%>" class="pull-right" aria-hidden="true" ><i class="uiIconClose uiIconWhite"></i></a>
              <span class="popupTitle center"><%=res.getString("UIRegisterForm.title")%></span>
          </div>
          <div class="popupContent">
              <% if (errors.size() > 0) { %>
              <div class="alert alert-error mgT0 mgB20">
                <ul>
                    <% for (String error : errors) { %>
                    <li><i class="uiIconError"></i><span><%=error%></span></li>
                    <%}%>
                </ul>
            </div>
            <%}%>
            <form name="registerForm" action="<%= contextPath + "/login"%>" method="post" style="margin: 0px;">
                <div class="content">
                    <div class="form-horizontal">
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.userName")%></label>
                            <div class="controls">
                                <input class="username <%if(errorFields.contains("username")) out.print(errorClass);%>" name="username" type="text" value="<%=(user.getUserName() == null ? "" : user.getUserName())%>" />
                                <span> *</span>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.password")%></label>
                            <div class="controls">
                                <input class="password <%if(errorFields.contains("password")) out.print(errorClass);%>" name="password" type="password" />
                                <span> *</span>
                            </div>
                        </div>
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.confirmPassword")%></label>
                            <div class="controls">
                                <input class="password <%if(errorFields.contains("password2")) out.print(errorClass);%>" name="password2" type="password" />
                                <span> *</span>
                            </div>
                        </div>
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.firstName")%></label>
                            <div class="controls">
                                <input type="text" class="<%if(errorFields.contains("firstName")) out.print(errorClass);%>" name="firstName" value="<%=(user.getFirstName() == null ? "" : user.getFirstName())%>"/>
                                <span> *</span>
                            </div>
                        </div>
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.lastName")%></label>
                            <div class="controls">
                                <input type="text" class="<%if(errorFields.contains("lastName")) out.print(errorClass);%>" name="lastName" value="<%=(user.getLastName() == null ? "" : user.getLastName())%>"/>
                                <span> *</span>
                            </div>
                        </div>
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.displayName")%></label>
                            <div class="controls">
                                <input type="text" class="<%if(errorFields.contains("displayName")) out.print(errorClass);%>" name="displayName" value="<%=(user.getDisplayName() == null ? "" : user.getDisplayName())%>"/>
                            </div>
                        </div>
                        <div class="control-group">
                            <label class="control-label"><%=res.getString("UIRegisterForm.label.emailAddress")%></label>
                            <div class="controls">
                                <input type="text" class="<%if(errorFields.contains("emailAddress")) out.print(errorClass);%>" name="email" value="<%=(user.getEmail() == null ? "" : user.getEmail())%>" />
                                <span> *</span>
                            </div>
                        </div>

                        <input type="hidden" name="login_controller" value="submit_register"/>
                    </div>
                </div>
                <div id="UIPortalLoginFormAction" class="uiAction">
                    <button type="submit" class="btn btn-primary"><%=res.getString("UIRegisterForm.action.SubscribeOAuth")%></button>
                    <button type="reset" class="btn ActionButton LightBlueStyle"><%=res.getString("UIRegisterForm.action.Reset")%></button>
                </div>
            </form>
        </div>
    </div>
	</div>
    </body>
</html>
