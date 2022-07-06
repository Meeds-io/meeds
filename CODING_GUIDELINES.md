# Coding Guidelines

This document is an extension to [CONTRIBUTING](./CONTRIBUTING.md) and provides more details about the coding guidelines and best practices to follow by commmitters.
It's meant to be used for code reviews with the aim at improving quality and consistency of the code. Failure to comply with these guidelines may result in rejection of pull requests.

To propose a change in the coding guidelines, simply submit a pull request on this document. make sure you give a unique ID to any guideline in the form of DEVSTD-<ID> so it can be easily refered.

## General

### DEVSTD-1	
  Always use English language in source code
  
### DEVSTD-9 
  Add comments to complex algorithms
  
### DEVSTD-10
  	Divide complex code and big services to smaller pieces
  
### DEVSTD-22
  	No commented code should be committed  
  
## Pull Requests
  
### DEVSTD-8
  Commits inside PRs must have clear description
  
### DEVSTD-18
  No whitespace changes or formatting changes has to be made in PRs. In fact, the PR can become unreadable with those changes.
  
### DEVSTD-19
  Before requesting a PR, ensure that the build passes and that the branch is up to date with destination branch
  
### DEVSTD-20
  When merging a PR, if it's about a fix or a quick win, the PR should be squached to a single commit and then merged. Else, if it's a Feature, a "Merge" commit should be added to destination branch
  
### DEVSTD-48	
  Use a clear commit message of format TASK-XYZ (replace XYZ by task number) and a clear description that explains:
1/ What was the previous behavior (Prior ti this change....)
2/ What is changed/impacted by the current change
3/ (Optional) what other alternative solutions/propositions was studied
  
  
## Java

### DEVSTD-2	
  Never use stderr and stdout (Exception.printStackTrace, System.out, System.err...)
  
### DEVSTD-3	
  Define Javadoc for all public classes, methods, attributes and constants
  
### DEVSTD-4	
  Use common eXo Code formatting rules
  
### DEVSTD-5	
  Format only modified code instead of the whole class
  
### DEVSTD-6	
  Avoid using of java.lang.String.intern()
  
### DEVSTD-7	
  String concatenation
  
### DEVSTD-11	
  Use eXoLogger for logging purpose
  
### DEVSTD-12	
  Never use == to compare Objects
  
### DEVSTD-23	
  When adding new methods in API, please make sure to add a default implementation to ensure that it doesn't break implementations Mocks in Tests for example
  
### DEVSTD-15	
  Add Unit tests. 
  When the issue is about a bug, the test has to fail wihout the fix and must succeeds with the proposed patch
  
### DEVSTD-16	
  Use and implement equals and hashcode methods to compare Objects
  
### DEVSTD-17	
  Use braces when using for, while, do/while, if / else and switch blocks
  
### DEVSTD-49	
  A module can depend only on the Service layer API of another module/project
  
### Exceptions  
  
### DEVSTD-21	
  No exception should be swallowed. A log.debug is sometimes sufficient, if the exception is expected
  
### DEVSTD-24	
  An exception should be either loggued or rethrown, not both
  
### DEVSTD-13	
  Never swallow exceptions
  
### DEVSTD-14	
  Always log the complete exception (avoid using exception.getMessage())
  

## Persistence  
  
### DEVSTD-27	
  Make sure that all RDBMS fields accepts emoticons characters and that it's displayed correctly in front-end only if explicitly not acceptable 
  
### DEVSTD-28	
  Avoid using org.exoplatform.services.security.ConversationState.getCurrent() in Service & Storage layers. 
  In fact, the access to the currently connected user should be done from higher layer such as REST Service / Portlet. 
  
### DEVSTD-32	
  Data structure upgrades of JPA Entities has to be made using Liquibase changelogs
  
### DEVSTD-33	
  Data content upgrades has to be made using Commons-Upgrade API (See link for more details)
  
### DEVSTD-52	
  Avoid using SessionProviderService.getSystemSessionProvider in Service & Bottom layer since this Service shares the session provider in a thread local in User HTTP Requests.
  


## REST APIs
  
### DEVSTD-36	
  Use @RolesAllowed annontation in ALL REST endpoint METHODs (not classes) in order to restrict access whether to all connected "users" or to "administrators" only (when this is about an administration operation).
Exceptions: in some cases we need to avoid adding @RolesAllowed to allow access to an endpoint, thus a token validation process MUST be made for anonymous users, see org.exoplatform.social.rest.impl.user.UserRestResourcesV1.getUserAvatarById AND org.exoplatform.agenda.rest.AgendaEventRest.sendEventResponse
  
### DEVSTD-37	
  Use HTMLSanitizer in REST endpoints when retrieving user data that will be displayed as HTML in Client Side. As example, Task Description or Activity.
  
### DEVSTD-38	
  The same form validations made on client-side MUST be made on server-side as well to avoid inconsistency when using endpoints in custom development
  
### DEVSTD-50	
  Avoid using @GET in REST endpoint for changing state operation. In fact only POST/PUT/DELETE/PATCH http methods are allowed to be used for changing state. This will avoid to have a CSRF attacks. We can for same exceptions use @GET for changing state methods, but only when a specific security mechanism is used, which can be equivalent to CSRF protection.

## Security
  
### DEVSTD-39	
  Never use Vue directive v-html on Data provided by user to avoid XSS attacks only when having already sanitized the content, where its content can be considered as safe. For example, writing an activity using WYSIWYG will produce an HTML that will be stored in Server Side, bu when displaying it, the HTMLSanitizer has to be used in Server-end (or similar tool in Client-side) before displaying activity content to the end user.
  
### DEVSTD-40	
  Encode HTTP parameters when displaying it in UI to avoid Non-persistent (reflected) XSS. When displaying in front-end a URL parameter, it should be encoded whether in server side (When it's displayed in JSP/GTMPL) or in client side (When using Javascript/Vue)
- For the server-side, we will have to use StringEscapeUtils.escapeHtml
- For the client side, we will have to use v-text or {{}} to display the information to make sure that it's escaped (Never use v-html here)
  
### DEVSTD-41	
  Use HTTP 404 response code instead of 403 when the URL uses username as path parameter or query parameter. This will avoid to give information about the existance of a User having the requested username
  
###  DEVSTD-42	
  Never give the possibility for Server to make HTTP requests to internet (adding a URL as parameter that will be fetched by Server for example or even conscieving a flow in the application that let Server making a flow to internet) => Avoid SSRF attacks
  
### DEVSTD-43	
  Avoid giving access to 'ANY' (anonymous access) to non static and non restricted resources. For example, default JCR nodes MUST grant access to authenticated users (/platform/users) at minimum
  
### DEVSTD-44	
  Avoid building manually a link, for example <a href="USER_DATA">, when the data is provided by user. Instead, you will be able to use 'v-autolinker' to transform a text link into a link element, the sanitization will be made by Autolinker library
  
### DEVSTD-45	
  Implement systematically in Service layer the ACL (permission checks) of all operations coming from REST layer. In other all Service layer calls from REST has to pass current user id and the Service layer has to implement the fine permission check algorithm
  
### DEVSTD-51	
  Avoid defining JSP files outside WEB-INF folder of WebARchives (.war files) to avoid security breach
  
## Extensibility  
  
### DEVSTD-46	
  Avoid defining html & gtmpl files in JARs in order to allow customize them in custom extensions
  
### DEVSTD-47	
  Separate interfaces from their implementation, put each in a separate jar. This will help define new implementations for available services depending on the defined architecture (Monoloith, Microservice, etc ...)
 - All services interfaces, Exceptions should be included in an API module (JAR)
 - All implementations and Utils classes sould be put inside Implementations module (JAR)


## Javascript
  
### DEVSTD-29	
  No use of npm import directive to define external third party libraries. All JS third party libraries has to be defined using GateIN AMD modules definition in gatein-resources.xml. When adding a new module, make sure to communicate about this new dependency and to add it in gatein-portal to let other addons reuse it, if the library is defined in a supported addon.

  
## Style
  
### DEVSTD-30	
  Avoid defining colors outside the file variables.less of platform-ui project.
  
### DEVSTD-31	
  Avoid defining new colors that wasn't specified in branding chart of eXo Platform by designers
  
### DEVSTD-34	
  When naming a Vue file, it has to be an explicit name that references the addon/project, the module/portlet and its name. This naming convension is like naming an FQN of a class. In fact, in eXo Platform, we have multiple Vue instances in a single page, by using this naming convension, we will avoid names collision, same as defining the same FQN of a java class in two jars.
  
### DEVSTD-35	
  Each portlet style has to be defined with parent ID style, example: #AwsomePortletId {...other styles...}. This will avoid applying styles on other elements outside the portlet and thus avoid regressions.
  
 ## Vue
  
### DEVSTD-25	
  Avoid adding CSS in Vue components and place it in gatein-resources.xml as portlet skin
  
### DEVSTD-26	
  Use "computed" properties as much as possible inside Vue components instead of objects attributes direct access

