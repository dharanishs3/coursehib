# CourseHub — Online Course & Training Management System

Spring Boot REST backend for the FTE fresher onboarding POC. This guide covers setup, running, architecture,
the API list, the SRS mapping, how it was verified, known limitations, and a demo script.

---

## 1. Technology

| Item | Used |
|---|---|
| Language / build | Java 21, Maven (wrapper `./mvnw`) |
| Framework | Spring Boot 4.1.2, Spring Web MVC |
| Database | MySQL 8 with Spring Data JPA / Hibernate |
| Security | Spring Security, JWT (JJWT 0.12.6, HS256, 24 h), BCrypt |
| Formats | JSON and XML (jackson-dataformat-xml) |
| Docs | Springdoc OpenAPI / Swagger UI |
| Other | Lombok, Jakarta Bean Validation |

---

## 2. Setup and run

**Prerequisites:** Java 21, MySQL running on `localhost:3306`.

1. Create the database once (the tables are created automatically by Hibernate on startup):
   ```sql
   CREATE DATABASE IF NOT EXISTS coursehub_db;
   ```
2. Check `src/main/resources/application.properties` (database user/password, JWT secret, admin account, upload folders).
3. Run from the **project root folder** (the `./upload` folder is relative to where the app starts):
   ```bash
   ./mvnw spring-boot:run
   ```
   or build and run the jar:
   ```bash
   ./mvnw clean package -DskipTests
   java -jar target/coursehub-0.0.1-SNAPSHOT.jar
   ```
4. Open:
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`
   - OpenAPI JSON: `http://localhost:8080/v3/api-docs`
5. **Default admin** is created at startup if missing (values in `application.properties`, keys `app.admin.*`):
   email `admin@coursehub.com`, password `Admin@12345`.
6. **Using Swagger with a token:** call `POST /api/auth/login`, copy the `token`, click **Authorize**, paste the token (no "Bearer " prefix needed).

### Configuration keys (all read with `@Value`)
| Key | Meaning |
|---|---|
| `spring.datasource.*` | MySQL connection |
| `app.jwt.secret`, `app.jwt.expiration-hours` | JWT signing key (min 32 chars) and lifetime in hours (24) |
| `app.admin.full-name / email / password` | Default admin created at startup |
| `app.storage.video-dir`, `app.storage.material-dir` | Upload folders (`./upload/videos`, `./upload/materials`) |
| `app.storage.allowed-video-extensions`, `allowed-material-extensions` | Allowed file types |
| `app.storage.max-material-size-mb` | Maximum material size (50 MB) |
| `spring.servlet.multipart.max-file-size` | Maximum upload size (500 MB, for videos) |
| `app.pagination.default-page-size`, `max-page-size` | Paging defaults (10 and 50) |

---

## 3. Architecture

Package `com.coursehub`:

| Package | Purpose |
|---|---|
| `controller` | HTTP only: read the request, call a service, return `ResponseEntity<Dto>` |
| `service` | Business rules and transactions |
| `repository` | Spring Data JPA interfaces (queries) |
| `entity`, `enums` | 10 JPA entities and 5 enums |
| `dto.request`, `dto.response` | Request/response classes (never entities) |
| `mapper` | Static entity → DTO methods |
| `helper` | `EnrollmentProgressHelper` (progress calculation), `CourseAccessHelper` (access checks), `RatingHelper` (average rating) |
| `util` | `FileStorageUtil` (safe file save/read/delete) |
| `security` | `JwtService`, `JwtAuthenticationFilter` (also checks the logout blacklist), `CustomUserDetails(Service)`, `CurrentUserService` |
| `config` | `SecurityConfig` (rules, BCrypt), `OpenApiConfig`, `DataInitializer` (default admin) |
| `exception` | Custom exceptions and `GlobalExceptionHandler` (`@RestControllerAdvice`, 7 short handlers) |

**Request flow:** client → `JwtAuthenticationFilter` (reads the Bearer token) → controller (`@PreAuthorize`, `@Valid`) → service
(rules, `@Transactional`) → repository → MySQL; the entity is mapped to a DTO and returned. Errors go to `GlobalExceptionHandler`,
which returns one common body: `timestamp, status, error, message, path, fieldErrors`.

### Database (10 tables)
`users`, `categories`, `courses`, `course_videos`, `course_materials`, `course_references`, `enrollments`, `lesson_progress`, `course_reviews`, `blacklisted_tokens` (logged-out JWTs).

- One-to-Many: User→Enrollments/Reviews, Category→Courses, Course→Videos/Materials/References/Enrollments/Reviews, Enrollment→LessonProgress, CourseVideo→LessonProgress. The child holds the foreign key (`@ManyToOne`, LAZY).
- Unique constraints: user email and phone, category name, (course, video sequence), (user, course) for enrollments and reviews, (enrollment, video) for progress.
- `CascadeType.ALL` only from Course to videos/materials/references. Enrollments and reviews are never cascaded.

### Key design decisions (good answers for the demo)
- **No `ServiceImpl` interfaces:** each service has one implementation, and the project guidelines said to avoid unnecessary interfaces.
- **Static mappers:** a mapper has no state or dependencies, so static methods are the simplest form.
- **DTO classes, not records,** and manual mapping (no MapStruct).
- **Simultaneous edits:** no row locking is used (kept simple). The database unique constraints still stop duplicate video numbers, enrollments and reviews, and a clash returns 409 "conflicts with existing data".
- **Files:** stored under a UUID name; only that name is stored in MySQL; clients never see paths; downloads and streams check the JWT and enrollment every time.
- **Progress** is always recalculated from the `lesson_progress` rows (count based), never incremented.

---

## 4. API summary (37 endpoints, all in Swagger)

Roles: **Public**, **Any** = any logged-in user, **Admin**, **Learner**. Every endpoint accepts and returns JSON or XML (`Content-Type` / `Accept`), except file/stream bytes.

### Authentication and profile
| Method | Endpoint | Role | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a learner (role is always LEARNER) |
| POST | `/api/auth/login` | Public | Login, returns JWT |
| POST | `/api/auth/logout` | Any | Logout: the token is saved in the `blacklisted_tokens` table and rejected from then on |
| GET | `/api/users/me` | Any | My profile |
| PUT | `/api/users/me` | Any | Update my name and phone |

### Categories
| POST | `/api/admin/categories` | Admin | Create |
|---|---|---|---|
| PUT | `/api/admin/categories/{categoryId}` | Admin | Update |
| GET | `/api/categories` | Any | List |
| DELETE | `/api/admin/categories/{categoryId}` | Admin | Delete (409 if courses use it) |

### Courses
| POST | `/api/admin/courses` | Admin | Create (starts as DRAFT) |
|---|---|---|---|
| GET | `/api/courses/{courseId}` | Any | Details (search by id) |
| GET | `/api/courses` | Any | Search/filter, paged (`courseName, category, instructorName, keyword, page, size, sortBy, sortDirection`) |
| PUT | `/api/admin/courses/{courseId}` | Admin | Update (not when ARCHIVED) |
| PATCH | `/api/admin/courses/{courseId}/status` | Admin | Change status |
| DELETE | `/api/admin/courses/{courseId}` | Admin | Delete (409 if enrolled, archive instead) |

### Videos
| GET | `/api/courses/{courseId}/videos` | Admin / enrolled learner | List, sorted by sequence |
|---|---|---|---|
| POST | `/api/admin/courses/{courseId}/videos/upload` | Admin | Upload file (`videoTitle`, `durationSeconds`, `videoFile`) |
| POST | `/api/admin/courses/{courseId}/videos/link` | Admin | Add external link |
| PUT | `/api/admin/courses/{courseId}/videos/{videoId}` | Admin | Update title, duration, order |
| DELETE | `/api/admin/videos/{videoId}` | Admin | Delete |
| GET | `/api/videos/{videoId}/stream` | Admin / enrolled learner | Stream uploaded video (Range, 206). Needs the Bearer header |

### Materials and references
| GET | `/api/courses/{courseId}/materials` | Admin / enrolled learner | List |
|---|---|---|---|
| POST | `/api/admin/courses/{courseId}/materials/upload` | Admin | Upload (`materialTitle`, `file`: pdf, docx, pptx, xlsx, txt; max 50 MB) |
| GET | `/api/materials/{materialId}/download` | Admin / enrolled learner | Download (Bearer token required) |
| DELETE | `/api/admin/materials/{materialId}` | Admin | Delete |
| GET | `/api/courses/{courseId}/references` | Admin / enrolled learner | List |
| POST | `/api/admin/courses/{courseId}/references` | Admin | Add (http/https only) |
| PUT | `/api/admin/references/{referenceId}` | Admin | Update |
| DELETE | `/api/admin/references/{referenceId}` | Admin | Delete |

### Enrollment and progress (Learner only)
| POST | `/api/courses/{courseId}/enroll` | Learner | Enroll (published courses only) |
|---|---|---|---|
| GET | `/api/enrollments/my` | Learner | My enrolled courses |
| GET | `/api/enrollments/{enrollmentId}/progress` | Owner | Completion status |
| PUT | `/api/enrollments/{enrollmentId}/videos/{videoId}/progress` | Owner | Mark a video completed / not completed |
| GET | `/api/enrollments/{enrollmentId}` | Owner | Complete learning page (videos with progress, materials, references, reviews) |

### Reviews
| GET | `/api/courses/{courseId}/reviews` | Any | Reviews, newest first, paged |
|---|---|---|---|
| POST | `/api/courses/{courseId}/reviews` | Enrolled learner | Submit rating 1–5 and optional comment |
| PUT | `/api/reviews/{reviewId}` | Owner | Update my review (`submittedAt` stays) |

---

## 5. Business rules in one place
- **Course status:** DRAFT → PUBLISHED/ARCHIVED, PUBLISHED → ARCHIVED, PUBLISHED → DRAFT only while nobody is enrolled. ARCHIVED is permanent and read-only.
- **Visibility:** learners see only PUBLISHED courses in the catalogue. An enrolled learner keeps access to an archived course. DRAFT courses are hidden from learners (404).
- **Delete course:** with enrollments → 409 (archive instead). Without enrollments → deleted with videos, materials, references and their files. The category is kept.
- **Delete category:** 409 while courses use it.
- **Videos:** the backend numbers them 1..N; reordering and deleting keep the numbers continuous; the course duration is the sum of video durations (typed by the admin).
- **Progress:** `completed videos / total videos × 100` (2 decimals). 0 completed = ENROLLED, some = IN_PROGRESS, all = COMPLETED (with `completedAt`). A course with no videos stays ENROLLED at 0.00. Adding or deleting a video recalculates every enrolled learner.
- **Reviews:** only enrolled learners, one per course, rating 1–5, no delete.
- **Security:** 401 for a missing, invalid or expired token; 403 for the wrong role or a record that is not yours. User ids always come from the token.

---

## 6. SRS mapping

### Evaluation criteria
| Criterion | Where |
|---|---|
| Spring REST APIs for the use cases | 37 endpoints, section 4 |
| Spring Boot, Maven, Hibernate/JPA, MySQL | `pom.xml`, entities, repositories |
| Coding standards | Standard Java naming, one class per file, small methods |
| Layers: Controller, Service, Repository, Model/DTO, Entity, Utility, Helper, Exception, Configuration | The packages in section 3 |
| Values from `application.properties` with `@Value` | Section 2 table; used in `JwtService`, `FileStorageUtil`, `DataInitializer`, `SecurityConfig`, `CourseMaterialService`, `CourseService`, `ReviewService`, and the controllers' default page size |
| Produce and consume XML and JSON | `jackson-dataformat-xml`; `produces`/`consumes` on every controller |
| One-to-Many associations | `@OneToMany(mappedBy)` in the entities, section 3 |
| Swagger/OpenAPI for all REST APIs | `/swagger-ui/index.html`, 37 operations, bearer authorization |
| `@RestControllerAdvice` exception handling | `GlobalExceptionHandler` |
| All APIs up during the presentation | Start MySQL, then the app (section 2) |

### Use cases
| # | Use case | API |
|---|---|---|
| 1 | User login | `POST /api/auth/login` |
| 2 | Register new user | `POST /api/auth/register` |
| 3 | Create new course | `POST /api/admin/courses` |
| 4 | Search course by id | `GET /api/courses/{courseId}` |
| 5 | Search by course name | `GET /api/courses?courseName=` |
| 6 | Search by category | `GET /api/courses?category=` |
| 7 | Search by instructor name | `GET /api/courses?instructorName=` |
| 8 | Filter by keyword | `GET /api/courses?keyword=` |
| 9 | Filtered courses in table view | Same endpoint returns a paged list (`content`, `totalElements`, `totalPages`); the table itself is the frontend's job |
| 10 | Admin add / update / delete courses | `POST/PUT/DELETE /api/admin/courses` |
| 11 | Add training videos to a course | `POST .../videos/upload` and `.../videos/link` |
| 12 | Add / update / delete categories | `/api/admin/categories` |
| 13 | Upload / delete videos or video links | `POST .../videos/upload`, `.../link`, `DELETE /api/admin/videos/{id}` |
| 14 | Upload / download course materials | `POST .../materials/upload`, `GET /api/materials/{id}/download` |
| 15 | Add external reference links | `POST /api/admin/courses/{id}/references` |
| 16 | Profile settings | `GET/PUT /api/users/me` |
| 17 | Enroll user into a course | `POST /api/courses/{id}/enroll` |
| 18 | View enrolled courses | `GET /api/enrollments/my` |
| 19 | Track course completion status | `GET /api/enrollments/{id}/progress` and `PUT .../videos/{videoId}/progress` |
| 20 | Submit rating and feedback | `POST /api/courses/{id}/reviews` |

All 20 use cases and all evaluation criteria are implemented. Nothing is missing.

---

## 7. How it was verified
No JUnit tests were written (as requested). Every API was checked with curl against the running application, and database state was checked with SQL.
About 700 checks passed across the phases (auth 180, videos 182, materials/references 171, enrollment/progress 160, reviews 109). They covered status codes, error messages, response fields, JSON and XML, roles, ownership, validation, parallel requests, file bytes, Range requests, and file cleanup.
The application also starts from an empty database (tables, foreign keys, unique constraints and the admin are created), and the packaged jar runs.

### Manual verification checklist (use Swagger or Postman)
1. `./mvnw clean compile` succeeds, and the app starts.
2. MySQL tables and foreign keys exist (`SHOW TABLES;`).
3. Swagger UI opens and lists the endpoints.
4. Register a learner; log in; copy the JWT.
5. Admin vs learner: a learner calling `/api/admin/...` gets 403; no token gets 401; a bad or expired token gets 401.
6. Duplicate email and duplicate category give 409.
7. Create/update a course; search with each filter and combined filters; try paging and sorting; change status through DRAFT → PUBLISHED → ARCHIVED.
8. Delete a course without enrollments (204), and one with enrollments (409). Archived course: enrolled learner still has access.
9. Enroll; enroll again (409); mark videos complete and watch the percentage (2 of 4 = 50, 3 of 5 = 60, all = 100 and COMPLETED); a course with no videos stays at 0.
10. Add, reorder and delete videos; check numbering, course duration, and learner progress changes.
11. Upload and download a material as admin and as an enrolled learner; a non-enrolled learner gets 403.
12. Stream an uploaded video, with a `Range: bytes=0-99` header (expect 206).
13. Submit and update a review; submitting twice gives 409.
14. Send a request body as XML with `Content-Type: application/xml` and `Accept: application/xml`.
15. Send invalid data (empty fields, rating 6) and check the 400 field errors.
16. Look at the application log for errors.

---

## 8. Known limitations
- **No automated test classes** (by request).
- Swagger UI was checked by loading its page and the API list, not by clicking through it in a browser.
- The 500 MB video limit was not tested at its limit. File types are checked by extension only, not by content.
- The video types `mp4, webm, mkv, mov` were chosen by me, because the requirements did not name them. Browser playback of `mkv`/`mov` depends on the browser.
- The `videoUrl` of an external video link is not checked for an `http://` or `https://` start (the check was removed from `VideoLinkRequest`). A value like `ftp://...` or plain text is accepted. Reference links are still checked.
- Video duration is typed by the admin (no automatic reading from the file).
- The database password and JWT secret are written in `application.properties`, as decided. For a real deployment they should come from environment variables.
- A browser `<video>` tag cannot send the Authorization header, so the frontend must load protected videos with `fetch` (Bearer header) and play the returned Blob, or the stream endpoint must be changed later.
- A 401 response has no body (Spring's built-in entry point). A 403 caused by the URL rule `/api/admin/**` uses Spring's default error body, while a 403 from the code uses the common error format.
- `%` and `_` typed in a search work as wildcards.
- Decimal numbers for whole-number fields (like rating `4.5` or `4.0`) are rejected with 400.
- Logout uses a token blacklist (the filter checks it on every request). There are no refresh tokens, password reset, or user management (not in the requirements). A disabled account keeps working until its token expires (there is no API to disable users).
- CORS is not configured: a React app on another port must use a dev proxy (for example the Vite proxy), or CORS must be added later.
- Without row locking, a rare double-click can return a plain 500 instead of 409 (for example a duplicate review). The data stays correct.
- Hibernate `ddl-auto=update` is used (fine for development; use migrations in production).
- Uploaded files live in `./upload` next to where the app is started. If a file is removed by hand, the API returns 404 for it.
- Without row locking, two admins changing the same course's videos at the same moment can get a 409 for one of them (they retry), and two parallel progress updates of one enrollment can leave a wrong percentage until the next update.

---

## 9. Demo script (about 8 minutes)
1. **Start:** show MySQL running, start the app, open Swagger (37 endpoints grouped by controller).
2. **Admin:** log in, Authorize. Create a category and a course (shows DRAFT). Add a video link and upload a small video. Upload a PDF material and add a reference link.
3. **Publish** the course, then show a draft/published difference with the learner view.
4. **Learner:** register, log in, search the catalogue (by name, keyword, instructor; paging), open details.
5. **Enroll;** open the learning page (`GET /api/enrollments/{id}`): videos, materials, references, reviews.
6. **Progress:** mark videos complete and show the percentage and status change. Add a video as admin and show the learner's percentage drop.
7. **Download** the material; show a different learner getting 403. Stream the video with a Range header.
8. **Review:** submit, try a second one (409), update it.
9. **Rules:** try deleting the enrolled course (409), archive it, show the learner still has access.
10. **XML:** repeat one call with `application/xml`.
11. **Code tour:** controller → service → repository → entity, `GlobalExceptionHandler`, `application.properties` with `@Value`.
