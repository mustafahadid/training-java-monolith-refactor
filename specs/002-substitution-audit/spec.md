# Feature Specification: Substitution Audit

**Feature Branch**: `002-substitution-audit`

**Created**: 2026-07-08

**Status**: Draft

**Input**: User description: "Specify a substitution audit over the rediscovery artifacts (phase 1). Deliverables: (1) a substitution-audit table with columns *legacy element*, *proposal* (keep-as-is | replace-with-library | replace-with-platform | retire), *reason*, *trade-off*; (2) coverage spanning home-grown code with modern equivalents, dated integrations, unfit data stores, EOL runtimes/frameworks, and cloud-hostile operational assumptions. Constraint: audit only — do not design the new architecture. Success: every row links back to a specific rediscovery entry."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Substitution Audit Table Creation (Priority: P1)

A modernization architect or technical lead needs to systematically evaluate every legacy element identified in the rediscovery phase and determine its modernization approach without designing the new architecture.

**Why this priority**: This is the foundation of the modernization decision-making process. Without a systematic evaluation of each legacy element, teams cannot plan phased migrations, estimate effort, or identify quick wins versus complex transformations.

**Independent Test**: Architect can review the substitution-audit table, locate any legacy element row, trace it back to the corresponding rediscovery artifact, and verify the proposal, reason, and trade-off are documented and justified.

**Acceptance Scenarios**:

1. **Given** a legacy element identified in rediscovery artifacts, **When** the architect locates it in the substitution-audit table, **Then** the row includes the element name, one of four proposal types (keep-as-is, replace-with-library, replace-with-platform, retire), a reason explaining the decision, and documented trade-offs
2. **Given** a substitution-audit table row, **When** the architect follows the rediscovery reference link, **Then** they can locate the specific rediscovery entry (business rule, data model, integration point, or open question) that documented the legacy element
3. **Given** a proposal type of "replace-with-library", **When** the architect reviews the reason, **Then** they can understand what category of library would replace it (e.g., "modern HTTP client", "standard date/time library") without seeing specific product names or implementation designs
4. **Given** a proposal type of "retire", **When** the architect examines the reason and trade-off, **Then** they can understand why the element is obsolete and what impact removing it may have

---

### User Story 2 - Comprehensive Coverage Validation (Priority: P2)

A migration planning team needs to verify that the substitution audit covers all critical areas of technical debt to ensure no major legacy components are overlooked in modernization planning.

**Why this priority**: Incomplete coverage leads to surprises during migration. Teams must know they've evaluated all home-grown utilities, dated integrations, data store choices, EOL technologies, and cloud-incompatible patterns before planning sprints or estimating effort.

**Independent Test**: Planning team can review the substitution-audit table and confirm it includes at least one entry from each required coverage area: home-grown code with modern equivalents, dated integrations, unfit data stores, EOL runtimes/frameworks, and cloud-hostile operational assumptions.

**Acceptance Scenarios**:

1. **Given** the rediscovery artifacts documented home-grown utilities or frameworks, **When** the team searches the substitution-audit table for these elements, **Then** they find entries evaluating each for replacement with standard libraries
2. **Given** the integration inventory identified external system connections, **When** the team reviews dated integrations in the audit table, **Then** they see proposals for integrations using deprecated protocols, obsolete APIs, or outdated authentication methods
3. **Given** the data-model summary revealed database technology and storage patterns, **When** the team examines data store entries in the audit table, **Then** they find evaluations of database versions, storage technologies, or persistence patterns that are EOL or unsuitable for modern deployment
4. **Given** the rediscovery documented runtime environments and frameworks, **When** the team searches for EOL runtime entries, **Then** they see audits of Java versions, application servers, framework versions, or libraries with published end-of-life dates
5. **Given** the rediscovery noted operational patterns like hard-coded paths or filesystem dependencies, **When** the team reviews cloud-hostile operational assumptions, **Then** they find entries evaluating patterns incompatible with containerized or cloud-native deployment

---

### User Story 3 - Traceability and Justification Review (Priority: P3)

A stakeholder or governance reviewer needs to validate that every audit decision is evidence-based, traceable to actual system findings, and includes documented reasoning for approval or challenge.

**Why this priority**: Governance and investment decisions require justification. Stakeholders must be able to trace audit recommendations back to concrete evidence and understand the reasoning to approve migration priorities, challenge proposals, or request additional analysis.

**Independent Test**: Reviewer can select any row in the substitution-audit table, follow the rediscovery reference, confirm the legacy element exists in rediscovery documentation, and evaluate whether the proposal reason and trade-offs are sufficient to justify the decision.

**Acceptance Scenarios**:

1. **Given** a substitution-audit row with a "replace-with-platform" proposal, **When** the reviewer follows the rediscovery reference, **Then** they can verify the legacy element was documented in rediscovery with enough detail to justify the proposal
2. **Given** a proposal reason stating a component is "end-of-life", **When** the reviewer examines the reason field, **Then** they see sufficient detail to understand which version is EOL and when support ends
3. **Given** a trade-off documenting migration complexity, **When** the reviewer reads the trade-off field, **Then** they understand the cost, risk, or effort implications without seeing implementation specifics
4. **Given** a "keep-as-is" proposal, **When** the reviewer reads the reason and trade-off, **Then** they can understand why modernization is not recommended and what ongoing costs or risks remain

---

### Edge Cases

- What happens when a legacy element fits multiple coverage areas? (Include it once in the most critical category, note cross-references in the reason or trade-off fields)
- How should partially-retired features be audited? (Audit the active portion with a proposal; document the retired portion's removal as an assumption or note)
- What if rediscovery artifacts are incomplete or missing for certain legacy elements? (Flag as an open question requiring additional rediscovery before audit can be completed)
- How should tightly-coupled legacy elements be audited? (Audit each element separately but note coupling in trade-offs; avoid designing the decoupling solution)
- What if a legacy element has no clear modern equivalent? (Proposal can be "keep-as-is" with reason explaining no suitable replacement exists; or "retire" if functionality is no longer needed)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST produce a substitution-audit table with columns: legacy element, proposal, reason, trade-off
- **FR-002**: Every row in the substitution-audit table MUST include a legacy element name or identifier exactly as documented in rediscovery artifacts
- **FR-003**: Every row MUST include one of four proposal values: "keep-as-is", "replace-with-library", "replace-with-platform", or "retire"
- **FR-004**: Every row MUST include a reason field explaining why the proposal was selected for that legacy element
- **FR-005**: Every row MUST include a trade-off field documenting costs, risks, complexity, or ongoing implications of the proposal
- **FR-006**: Every row MUST include a reference or link back to the specific rediscovery entry (business rule, data model entity, integration point, or open question) that documented the legacy element
- **FR-007**: Substitution audit MUST include coverage of home-grown code that has modern library equivalents (e.g., custom date utilities, proprietary HTTP clients, hand-rolled validation frameworks)
- **FR-008**: Substitution audit MUST include coverage of dated integrations (e.g., deprecated protocols, obsolete external APIs, outdated authentication methods, legacy message formats)
- **FR-009**: Substitution audit MUST include coverage of unfit data stores (e.g., EOL database versions, storage technologies unsuitable for modern deployment, persistence patterns incompatible with cloud environments)
- **FR-010**: Substitution audit MUST include coverage of EOL runtimes and frameworks (e.g., Java versions past end-of-support, application servers with published EOL dates, framework versions no longer maintained)
- **FR-011**: Substitution audit MUST include coverage of cloud-hostile operational assumptions (e.g., hard-coded filesystem paths, assumptions of persistent local storage, node-specific configuration, single-server deployment patterns)
- **FR-012**: Audit documentation MUST NOT include new architecture designs, implementation plans, or technology selection decisions
- **FR-013**: Audit documentation MUST NOT specify concrete replacement products, frameworks, or platforms (e.g., avoid "replace with Spring Boot", prefer "replace-with-platform" category with reason "current framework is EOL")
- **FR-014**: When encountering ambiguous legacy elements or unclear rediscovery data, audit work MUST stop and clarification MUST be requested rather than making assumptions about proposals

### Key Entities *(include if feature involves data)*

- **Legacy Element**: A component, pattern, technology, or operational assumption identified in rediscovery artifacts that requires modernization evaluation (examples: custom date utility class, WebSphere configuration, hard-coded JDBC connection string)
- **Proposal**: One of four standardized audit decisions:
  - "keep-as-is": Element remains unchanged in modernized system
  - "replace-with-library": Element should be replaced with a standard industry library
  - "replace-with-platform": Element should be replaced with platform-provided capability (e.g., cloud service, application server feature)
  - "retire": Element is obsolete and should be removed without replacement
- **Reason**: Text explanation of why the proposal was selected, including relevant context like EOL dates, industry best practices, or business requirements
- **Trade-off**: Text documentation of costs, risks, complexity, ongoing maintenance burden, or other implications of the proposal
- **Rediscovery Reference**: Link or citation connecting the audit row back to the specific rediscovery artifact (file path, section heading, entity name, integration ID) that documented the legacy element

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every legacy element documented in rediscovery artifacts has a corresponding row in the substitution-audit table
- **SC-002**: 100% of substitution-audit table rows include a valid rediscovery reference that can be traced back to business rules, data model, integration inventory, or open questions
- **SC-003**: Substitution-audit table includes at least one entry from each of five coverage areas: home-grown code with modern equivalents, dated integrations, unfit data stores, EOL runtimes/frameworks, and cloud-hostile operational assumptions
- **SC-004**: 100% of audit rows include all required fields: legacy element, proposal (one of four valid types), reason, and trade-off
- **SC-005**: Technical reviewers can validate any audit row by following the rediscovery reference and confirming the legacy element exists as documented
- **SC-006**: Audit documentation contains zero architecture designs, zero implementation plans, and zero specific technology selections (measurable via keyword scan for product names, implementation details, or design patterns)

## Assumptions

- Rediscovery artifacts (business rules, data model, integration inventory, open questions) are complete and accurate enough to support substitution audit
- Reviewers and stakeholders understand the four proposal categories and their intent
- "Replace-with-library" and "replace-with-platform" proposals can describe categories of replacements without specifying products (e.g., "modern HTTP client library" vs "OkHttp 4.x")
- EOL dates for runtimes and frameworks are publicly available and trustworthy (vendor support statements, community-maintained EOL lists)
- Legacy elements not documented in rediscovery artifacts are out of scope for this audit (additional rediscovery required first)
- Cloud-hostile operational assumptions are defined as patterns incompatible with containerized, ephemeral, or multi-tenant deployment models
- Audit is a decision-making artifact, not an implementation plan; actual migration design and sequencing occur in subsequent phases
