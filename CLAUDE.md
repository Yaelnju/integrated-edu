# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Who is working here

**Student 2 (College B / Oracle).** Basic Java + OOP knowledge, no prior JDBC/XML/XSD/XSL experience. Explain new concepts briefly before generating code.

## What is being built

A 3-college federated academic system: each college runs its own DBMS and Java app; a central Integration Server brokers cross-college enrollment via XML. Heterogeneity (different schemas, field names, DBMSes) is intentional.

| Module | College | DBMS | GUI port | XML port | Status |
|--------|---------|------|----------|----------|--------|
| `college-a` | A – Computer Science | SQL Server | 9002 | 9102 | done |
| **`college-b`** | **B – Chemistry** | **Oracle** | **9001** | **9101** | mostly done |
| `college-c` | C – reference impl | MySQL | 9000 | 9100 | done |
| `integration-server` | central hub | — | 9200 | — | done |
| `common` | shared utilities | — | — | — | done |

## Build & run commands

```bash
# Build all modules (skip tests for speed)
mvn -q clean package -DskipTests

# Run College B GUI + servers (from repo root)
mvn -pl college-b -q compile exec:java \
    -Dexec.mainClass=cn.nju.dataintegration.collegeb.CollegeBApplication

# Run Integration Server
mvn -pl integration-server -q compile exec:java \
    -Dexec.mainClass=cn.nju.dataintegration.integration.IntegrationApplication

# Run all tests
mvn clean test

# Run tests for one module
mvn -pl integration-server test
```

## Database setup

All three DB containers can be started together:
```powershell
.\scripts\start-dbs.ps1    # starts oracle-xe, sqlserver, mysql containers
.\scripts\load-schemas.ps1 # runs 01_schema.sql + 02_seed.sql for each college
```

College B (Oracle) connection details:
- **JDBC URL**: `jdbc:oracle:thin:@localhost:1521/FREEPDB1`
- **User**: `collegeb` / **Password**: `collegeb123`
- Service name is `FREEPDB1` (the pluggable DB in `gvenzl/oracle-free`), not `FREE` or `XE`

## Module layout

```
integrated-edu/
├── common/              shared: XmlFrameProtocol, XmlSchemaValidator
├── integration-server/  TCP hub (port 9200), no JDBC, hosts all XSD+XSL files
├── college-a/           SQL Server
├── college-b/           Oracle  ← your code
└── college-c/           MySQL
```

Each college module has:
- `sql/01_schema.sql` + `sql/02_seed.sql` — DDL + 50 students / 10 courses / 250 enrollments
- `net/CollegeXTcpServer` — business commands (GUI port), `net/XmlTcpServer` — XML export/writeback
- `repo/CollegeXRepository` — all JDBC queries
- `xml/DomXmlExporter` — builds DOM4J XML from ResultSets, using JDBC column names as element names
- `gui/` — Swing LoginFrame + MainFrame via CollegeXClient (single TCP call per action)

## Architecture: wire protocol

`XmlFrameProtocol` (in `common/`) — line-based, `|`-separated, XML wrapped in `<XMLBEGIN>` / `<XMLEND>`:

```
→ LOGIN|B24000001|123456\n
← OK\n<XMLBEGIN>\nSTUDENT|B24000001\n<XMLEND>\n
← ERR|message\n   (on failure)
```

## Architecture: cross-college enrollment

```
B GUI  →  CROSS_ENROLL|sno|cno|A  →  CollegeBTcpServer (9001)
            ↓ relays
        IntegrationTcpServer (9200) via CrossEnrollService
            ↓ fetches GET_COURSES from target XML port
            ↓ validates XSD  →  transforms XSL (local → unified → target)
            ↓ sends ENROLL|sno|cno to target XML port (9102 for A)
        XmlTcpServer at target: ensureStudentForCross(sno), then INSERT enrollment
```

DROP goes to local college first, then business server notifies integration (`INTEGRATED_DROP`) so both the student's home college and the course-owning college remove the record.

## College B data model (Oracle)

```sql
STUDENT(STU_NO VARCHAR2(9) PK, STU_NAME VARCHAR2(10 CHAR), SEX CHAR(1), MAJOR VARCHAR2(16 CHAR), PWD VARCHAR2(6))
COURSE(CRS_NO VARCHAR2(5) PK, CRS_NAME VARCHAR2(16 CHAR), PERIODS NUMBER(3), CREDIT NUMBER(1),
       TEACHER VARCHAR2(10 CHAR), LOCATION VARCHAR2(20 CHAR), SHARED CHAR(1) DEFAULT '0')
ENROLLMENT(CRS_NO VARCHAR2(5), STU_NO VARCHAR2(9), SCORE NUMBER(3), PRIMARY KEY(CRS_NO, STU_NO))
ACCOUNT(ACC_NO VARCHAR2(12) PK, ACC_PWD VARCHAR2(12), ACC_LEVEL NUMBER(2), STU_NO VARCHAR2(9) FK)
```

**Oracle pitfall**: columns storing Chinese must use `VARCHAR2(N CHAR)` (character semantics). The default byte semantics makes `VARCHAR2(10)` hold only ~3 Chinese characters because UTF-8 uses 3 bytes per character.

## Canonical XML format (fixed by Student 3)

```xml
<Students><student><id/><name/><sex/><major/></student></Students>
<Classes><class><id/><name/><time/><score/><teacher/><location/><share/></class></Classes>
<Choices><choice><sid/><cid/><score/></choice></Choices>
```

XSD for unified format: `integration-server/src/main/resources/xsd/integration/`  
XSL transforms (all colleges ↔ unified): `integration-server/src/main/resources/xsl/integration/`  
College B local XSDs: `college-b/src/main/resources/xsd/college-b/`

## Field mapping: College B DB → XML element names

`DomXmlExporter` uses JDBC column names directly as element names (uppercase Oracle names). XSL files must map these to canonical element names:

| Concept | B DB column | B local XML | Unified XML |
|---------|-------------|-------------|-------------|
| Student ID | `STU_NO` | `<STU_NO>` | `<id>` |
| Name | `STU_NAME` | `<STU_NAME>` | `<name>` |
| Sex | `SEX` | `<SEX>` | `<sex>` |
| Major | `MAJOR` | `<MAJOR>` | `<major>` |
| Course ID | `CRS_NO` | `<CRS_NO>` | `<id>` |
| Course hours | `PERIODS` | `<PERIODS>` | `<time>` |
| Credits | `CREDIT` | `<CREDIT>` | `<score>` |
| Shared flag | `SHARED` | `<SHARED>` | `<share>` |
| Enrollment SID | `STU_NO` | `<STU_NO>` | `<sid>` |
| Enrollment CID | `CRS_NO` | `<CRS_NO>` | `<cid>` |
| Grade | `SCORE` | `<SCORE>` | `<score>` |
