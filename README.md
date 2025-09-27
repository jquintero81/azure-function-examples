# Azure Durable Java MFA — Hexagonal Single-Project (Graph + B2C) — Complete Code

This document contains a single-module Java (Maven) project with hexagonal architecture. It is adapted so that users and OTP are persisted in Azure AD B2C using Microsoft Graph (Client Credentials). It includes Sagas/compensations and DI via SPI (Guice).

---

## Project Structure (summary)

```
src/main/java/com/example/hexagonal/
  core/
    model/
      User.java
      MfaChallenge.java
    port/
      out/
        UserRepositoryPort.java
        MfaRepositoryPort.java
  application/
    service/
      LoginService.java
      SagaCoordinator.java
  infrastructure/
    graph/
      AzureAdB2cUserRepository.java
    notification/
      LoggingNotificationAdapter.java
  functions/
    HttpTriggers.java
    LoginOrchestrator.java
    Activities.java
  di/
    FunctionsModule.java
    SimpleContainer.java
    FunctionInstanceInjectorImpl.java
resources/
  host.json
  local.settings.json
  META-INF/services/com.microsoft.azure.functions.worker.spi.FunctionInstanceInjector
pom.xml
README.md
```

---

> **Important**: This project is an integration example. Before using in production:
> - Do not store secrets in `local.settings.json`; use Key Vault or Managed Identity.
> - Adjust error handling and retries; add structured logging and monitoring.

---

## README (steps summary)

- Configure App Registration with Application permissions `User.ReadWrite.All` and admin consent.
- Set environment variables in `local.settings.json` (client id, secret, tenant, ext app id).
- Run `mvn clean package` and `mvn azure-functions:run`.
- POST to `/api/StartLoginOrchestration` with `{ "username": "<userPrincipalName or id>" }`.
- Check logs; get the code from the extension attribute (or notification in production).
- POST to `/api/RaiseMfaCodeEvent` with `{ "instanceId": "<id>", "code": "<code>" }`.

---

## Final considerations

- `AzureAdB2cUserRepository` uses `additionalDataManager()` to read/write custom attributes (extension properties). Confirm the exact prefix name in your tenant.
- For better security in Azure, use Managed Identity and `DefaultAzureCredential` instead of client secret; if you use MI, assign the necessary roles.
- Implement retries and backoff policies in Graph calls; the demo makes simple synchronous calls.

---
## Recommended steps to get started from scratch with Azure Toolkit in IntelliJ

**Install Azure Toolkit in IntelliJ**

- Go to `File → Settings → Plugins → Marketplace`.
- Search for `Azure Toolkit for IntelliJ` and install it.
- Restart IntelliJ.

**Create a new Azure Functions project**

- `File → New → Project → Azure Functions` (this option appears after installing the plugin).
- Choose Maven as the dependency manager.
- Select Java 11 or 17 as the runtime.
- The toolkit will ask for:
    - Group, artifact, and version (e.g. `com.example:azure-b2c-mfa:1.0-SNAPSHOT`).
    - Initial trigger: you can choose HTTP Trigger.

This generates:

- A minimal `pom.xml` with dependencies.
- A class with `@FunctionName`.
- Empty `host.json` and `local.settings.json` files.

**Verify the project starts**

Open a terminal in the project and run:

```bash
mvn clean package
func start
```

Check that your HTTP Function responds at http://localhost:7071/api/....

**Configure local.settings.json**
Add AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET (or prepare Managed Identity later).

**Debug in IntelliJ**
Create a remote debug configuration (address=5005).
Start with:

```bash
mvn clean package
func start --language-worker -- "--agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

Connect from IntelliJ and set breakpoints.

---

## Installing Azure Functions Core Tools v4 on Ubuntu (20.04 / 22.04)

### Installation with npm (Ubuntu 24.04)

#### Install Node.js (version ≥ 16):

```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

#### Install Functions Core Tools v4 globally:

```bash
npm install -g azure-functions-core-tools@4 --unsafe-perm true
```

#### Verify:

```bash
func --version
```

You should see something like `4.0.5455` or higher.

---

### Correct Final Package

Make sure to run `func start` from the correct directory:

- If you are using Maven, build the project with:

  ```bash
  mvn clean package
  ```

- Then start the function in the folder where Maven placed the package:

  ```bash
  cd target/azure-functions/<PROJECT_NAME>
  func start
  ```

⚠️ **Warning**: If you run `func start` from the root directory, it may fail to find the compiled classes.

---

## Custom B2C Policy Initiates the Login Flow

- From the policy (in an `OrchestrationStep`), call the `StartLoginOrchestration` Function via REST.
- This starts the Durable Orchestration (`LoginWithMfaOrchestrator`) and returns an `instanceId`.
- Store this `instanceId` as a temporary claim within the policy for later use.
- B2C displays the OTP input screen to the user.
- The user enters the code they received (via email, SMS, push notification, etc.).
- In the next step of the same Custom Policy, a REST call is made to the `RaiseMfaCodeEvent` Function.
- The `instanceId` and the code entered by the user are passed in this call.
- This step is not performed directly by the user; B2C executes it as part of the policy flow.
- The orchestrator (`LoginWithMfaOrchestrator`), which was waiting with `ctx.waitForExternalEvent("MfaCode")`, receives the event and continues the flow.

---

## Example of a Custom Policy (XML Fragment) for MFA flow

### 1. Claims We Need

```xml
<ClaimsSchema>
  <!-- Claim to store the instanceId of the Durable Function -->
  <ClaimType Id="durableInstanceId">
    <DisplayName>Durable Orchestration Instance ID</DisplayName>
    <DataType>string</DataType>
  </ClaimType>

  <!-- Claim for the OTP code entered by the user -->
  <ClaimType Id="mfaCode">
    <DisplayName>MFA Code</DisplayName>
    <DataType>string</DataType>
    <UserHelpText>Enter the verification code you received.</UserHelpText>
  </ClaimType>
</ClaimsSchema>
```

---

### 2. REST Technical Profile to Send the Code to the Function

```xml
<TechnicalProfile Id="REST-RaiseMfaCodeEvent">
  <DisplayName>Raise MFA Code Event</DisplayName>
  <Protocol Name="Proprietary" Handler="Web.TPEngine.Providers.RestfulProvider" />
  <Metadata>
    <Item Key="ServiceUrl">https://<YOUR_FUNCTION_APP>.azurewebsites.net/api/RaiseMfaCodeEvent</Item>
    <Item Key="AuthenticationType">None</Item>
    <Item Key="SendClaimsIn">Body</Item>
    <Item Key="HttpBinding">POST</Item>
  </Metadata>
  <InputClaims>
    <InputClaim ClaimTypeReferenceId="durableInstanceId" PartnerClaimType="instanceId" />
    <InputClaim ClaimTypeReferenceId="mfaCode" PartnerClaimType="code" />
  </InputClaims>
  <OutputClaims>
    <!-- You can capture the response here if needed -->
  </OutputClaims>
</TechnicalProfile>
```

---

### 3. Screen to Capture the OTP

```xml
<OrchestrationStep Order="2" Type="ClaimsExchange">
  <Preconditions>
    <Precondition Type="ClaimEquals" ExecuteActionsIf="false">
      <Value>isActiveMFASession</Value>
      <Value>true</Value>
      <Action>SkipThisOrchestrationStep</Action>
    </Precondition>
  </Preconditions>
  <ClaimsExchanges>
    <ClaimsExchange Id="SelfAsserted-MfaCode" TechnicalProfileReferenceId="SelfAsserted-MfaCodeInput" />
  </ClaimsExchanges>
</OrchestrationStep>
```

Where `SelfAsserted-MfaCodeInput` is a SelfAsserted Technical Profile that displays a textbox for the user to enter the OTP:

```xml
<TechnicalProfile Id="SelfAsserted-MfaCodeInput">
  <DisplayName>Enter MFA Code</DisplayName>
  <Protocol Name="Proprietary" Handler="Web.TPEngine.Providers.SelfAssertedAttributeProvider" />
  <Metadata>
    <Item Key="ContentDefinitionReferenceId">api.selfasserted</Item>
  </Metadata>
  <InputClaims>
    <InputClaim ClaimTypeReferenceId="mfaCode" />
  </InputClaims>
  <OutputClaims>
    <OutputClaim ClaimTypeReferenceId="mfaCode" Required="true" />
  </OutputClaims>
</TechnicalProfile>
```

---

### 4. OrchestrationStep to Send the Code to the Function

```xml
<OrchestrationStep Order="3" Type="ClaimsExchange">
  <ClaimsExchanges>
    <ClaimsExchange Id="RaiseMfaCodeEvent" TechnicalProfileReferenceId="REST-RaiseMfaCodeEvent" />
  </ClaimsExchanges>
</OrchestrationStep>
```

---

### 🔑 Complete Flow:

1. In step 1 (not shown here), the Durable Function is started, and the `durableInstanceId` is stored in a claim.
2. In step 2, the user is presented with the screen to enter the `mfaCode`.
3. In step 3, the policy calls the `REST-RaiseMfaCodeEvent` Technical Profile, sending `{ "instanceId": "...", "code": "..." }` to your Function.
4. The Durable Functions orchestrator receives the event and continues the flow.
