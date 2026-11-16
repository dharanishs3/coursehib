# CourseHub FTE Fresher Onboarding POC — Full Implementation Plan (for Sonnet)

This file is the complete, single source of truth for the build. It covers every point of the master prompt, the SRS evaluation criteria and the SRS API use cases. Work phase by phase. After each phase, stop and wait for the user's approval (per the master prompt, section 24/25). Do not skip rules. Do not add anything not listed here without asking.

---

## 0. Locked decisions (from the user)

| Decision | Value |
|---|---|
| Base package | `com.coursehub` (current code is `org.coursehub` → move everything to `com.coursehub`) |
| Phone number | Optional, **unique** (nullable; MySQL allows multiple NULLs) |
| Video duration | **Manual entry by ADMIN** for uploads and links. Do NOT use FFprobe. Do NOT install anything. |
| Entities/fields | Use the **master prompt** names (not the old implementation tracker) |
| Test code | No JUnit, Mockito, or test classes. Delete `CoursehubApplicationTests` and remove the four `*-test` starters from pom.xml |
| Source of truth | Master prompt + SRS evaluation criteria + SRS use cases (below) |

---

## 1. Technology stack (must follow exactly)

- Java 21, Spring Boot 4.1.2 (do NOT downgrade), Maven.
- MySQL, Spring Data JPA / Hibernate.
- Spring Web MVC (NOT WebFlux).
- Spring Security with JWT Bearer tokens (JJWT). BCrypt for passwords.
- Jakarta Bean Validation.
- Lombok (only where it reduces boilerplate). Avoid `@Data` on entities.
- Springdoc OpenAPI (Swagger) compatible with Boot 4.1.2 (current `springdoc-openapi-starter-webmvc-ui` 3.2.0 — verify with Maven).
- JSON AND XML request/response (Jackson XML compatible with Boot 4 / Jackson 3).
- DTO classes, NOT Java records. Manual mapper methods. NO MapStruct, NO ModelMapper.
- Frontend (React) is future scope — backend only. No React/JS code.
- Do NOT add: JUnit, Mockito, MapStruct, ModelMapper, Thymeleaf, WebFlux, Redis, extra frameworks, FFprobe.

---

## 2. Existing project state (inspected)

- `pom.xml`: Boot 4.1.2 parent, Java 21, deps present: data-jpa, security, validation, webmvc, springdoc 3.2.0, devtools (runtime), mysql-connector-j (runtime), lombok (optional + annotation processor configured in maven-compiler-plugin), and four `*-test` starters (REMOVE them).
- MISSING: `jackson-dataformat-xml` (Jackson 3 artifact; check the version Boot manages), JJWT (api/impl/jackson).
- `src/main/resources/application.properties`: MySQL URL `jdbc:mysql://localhost:3306/coursehub_db`, username `root`, **hardcoded password** (must move to env var `DB_PASSWORD`), `ddl-auto=update`, `show-sql=true`. Missing: JWT config, storage config, multipart limits.
- Java: `org/coursehub/CoursehubApplication.java` (move to `com/coursehub/`), `src/test/.../CoursehubApplicationTests.java` (delete).
- `target/` and `.idea/` exist — leave them alone.

---

## 3. Package structure (create only what is needed; no empty packages)

Base: `com.coursehub`

```
com.coursehub
├── CoursehubApplication.java
├── config        → SecurityConfig, OpenApiConfig, (XML/Jackson config if needed), StorageConfig (reads @Value)
├── controller    → AuthController, UserController, CategoryController, CourseController,
│                   CourseVideoController, CourseMaterialController, CourseReferenceController,
│                   EnrollmentController, ReviewController
├── dto
│   ├── request   → RegisterRequest, LoginRequest, UpdateProfileRequest, CategoryRequest,
│   │               CourseRequest, CourseStatusRequest, VideoUploadRequest(multipart), VideoLinkRequest,
│   │               VideoUpdateRequest, ReferenceRequest, VideoProgressRequest, ReviewRequest
│   └── response  → AuthResponse, UserResponse, CategoryResponse, CourseListResponse,
│                   CourseDetailsResponse, CourseVideoResponse, CourseMaterialResponse,
│                   CourseReferenceResponse, EnrollmentResponse, EnrollmentProgressResponse,
│                   EnrollmentDetailsResponse, ReviewResponse, ReviewSummaryResponse, ErrorResponse
├── entity        → User, Category, Course, CourseVideo, CourseMaterial, CourseReference,
│                   Enrollment, LessonProgress, CourseReview
├── enums         → UserRole, DifficultyLevel, CourseStatus, VideoSourceType, EnrollmentStatus
├── repository    → one JpaRepository per entity
├── service       → AuthService, UserService, CategoryService, CourseService, CourseVideoService,
│                   CourseMaterialService, CourseReferenceService, EnrollmentService, ReviewService
├── security      → CustomUserDetails, CustomUserDetailsService, JwtService, JwtAuthenticationFilter, CurrentUserService
├── mapper        → manual mapper classes (e.g., CourseMapper, VideoMapper, ...) with static methods
├── exception     → custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
└── util          → FileStorageService (or util class), SafeFileNameUtil, PagingUtil (if needed)
```
SRS "Helper/Utility" layers map to `util` (and optionally `helper`); keep only what is used.

---

## 4. Database entities (9, exactly as in master prompt section 8)

General: `id` Long PK, `@GeneratedValue(strategy = IDENTITY)`. Use `@Enumerated(EnumType.STRING)`. LocalDateTime timestamps set via `@PrePersist` / `@PreUpdate`.

### 4.1 User — table `users`
- id (Long), fullName (NotBlank, max 150), email (NotBlank, Email, max 150, UNIQUE, case-insensitive uniqueness: store lowercase/trim and check existence with lowercase), password (BCrypt hash, max 255), phoneNumber (optional, max 20, **UNIQUE**, nullable), role (UserRole: ADMIN, LEARNER), enabled (Boolean, default true), createdAt, updatedAt.
- Rules: password never plain; public registration ALWAYS LEARNER (ignore/forbid any role in request); no profile image; single fullName.

### 4.2 Category — table `categories`
- id, categoryName (NotBlank, max 100, UNIQUE, case-insensitive), description (max 500, optional).
- Delete is blocked (409) if any course references it. Never cascade-delete courses.

### 4.3 Course — table `courses`
- id, courseName (NotBlank, max 200; duplicates allowed), description (NotBlank, TEXT), category (ManyToOne, mandatory), instructorName (NotBlank, max 150), difficultyLevel (DifficultyLevel), language (NotBlank, max 50), durationSeconds (Long, default 0; = sum of video durations), status (CourseStatus, default DRAFT), createdAt, updatedAt.
- Enums: DifficultyLevel {BEGINNER, INTERMEDIATE, ADVANCED}; CourseStatus {DRAFT, PUBLISHED, ARCHIVED}.
- NO createdBy, thumbnail, price fields.

### 4.4 CourseVideo — table `course_videos`
- id, course (ManyToOne), videoTitle (NotBlank, max 200), videoSourceType (VideoSourceType: UPLOAD, EXTERNAL_LINK), videoUrl (NotBlank, max 1000; for UPLOAD = internal storage key/path, never returned to client; for EXTERNAL_LINK = the URL), sequenceNumber (Integer, not null, unique within course — DB unique constraint on course_id + sequence_number), durationSeconds (Integer, mandatory, positive — **manual entry**).
- Rules: sequence generated by backend (max+1 on add); never accept sequenceNumber on add. On reorder: validate, shift others, keep 1..N continuous. On delete: remove LessonProgress, close gap (renumber 1..N), recalc course duration, recalc enrollments.
- Update must NOT change source type or video URL.
- Duration recalculated (sum) after add/update/delete.

### 4.5 CourseMaterial — table `course_materials`
- id, course (ManyToOne), materialTitle (NotBlank, max 200), fileName (NotBlank, max 255), fileUrl (NotBlank, max 1000 = internal storage path/key; never exposed), contentType (NotBlank, max 100), fileSize (Long).
- Allowed extensions: PDF, DOCX, PPTX, XLSX, TXT only. No ZIP.

### 4.6 CourseReference — table `course_references`
- id, course (ManyToOne), referenceTitle (NotBlank, max 200), referenceUrl (NotBlank, max 1000, must be http or https). No duplicate-URL restriction.

### 4.7 Enrollment — table `enrollments`
- id, user (ManyToOne), course (ManyToOne), enrolledAt (auto), status (EnrollmentStatus: ENROLLED, IN_PROGRESS, COMPLETED), completionPercentage (BigDecimal, default 0.00), completedAt (nullable).
- UNIQUE(user_id, course_id). No CANCELLED status.

### 4.8 LessonProgress — table `lesson_progress`
- progressId (Long PK), enrollment (ManyToOne), courseVideo (ManyToOne), completed (Boolean, default false), completedAt (nullable).
- UNIQUE(enrollment_id, course_video_id). Video must belong to enrollment's course. When completed true → set completedAt; false → clear completedAt. No watch-duration fields.

### 4.9 CourseReview — table `course_reviews`
- reviewId (Long PK), user (ManyToOne), course (ManyToOne), rating (Integer, 1–5), reviewComment (TEXT, optional), submittedAt (auto, never changed on update).
- UNIQUE(user_id, course_id). No delete API.

---

## 5. Relationships

- User → Enrollments (OneToMany), User → CourseReviews (OneToMany)
- Category → Courses (OneToMany, mappedBy)
- Course → Category (ManyToOne), Course → CourseVideos, CourseMaterials, CourseReferences, Enrollments, CourseReviews (OneToMany, mappedBy)
- CourseVideo → LessonProgress (OneToMany), Enrollment → LessonProgress (OneToMany)
- Child side owns FK via @ManyToOne + @JoinColumn. Use LAZY. Avoid unconditional `CascadeType.REMOVE` (it bypasses the delete rule in section 19).
- DTOs everywhere; never serialize entities (no recursion).

---

## 6. Security (master prompt section 10)

- Roles: ADMIN, LEARNER. Public: `POST /api/auth/register`, `POST /api/auth/login`. Everything else needs auth. Swagger/OpenAPI docs URLs may be public.
- Components: SecurityConfig, CustomUserDetails, CustomUserDetailsService, JwtService, JwtAuthenticationFilter, CurrentUserService.
- `@EnableMethodSecurity` + `@PreAuthorize` for role checks.
- JWT: HS256, 24-hour expiry, secret from env (`JWT_SECRET`, min 32 bytes), not in source. Principal exposes userId.
- Invalid/expired JWT → 401. Authenticated but wrong role/ownership → 403.
- Never trust userId from request body or frontend; use principal (CurrentUserService).
- No refresh tokens, blacklist, password reset, logout.

---

## 7. API conventions (section 11)

- Base path `/api`. REST naming. Return response DTOs (never entities).
- Success: return the DTO directly (no `{success, message, data}` wrapper).
- Errors: one common `ErrorResponse`: timestamp, status, error, message, path, optional field validation errors.
- Status codes: 200 retrieve/update; 201 created; 204 delete (no body); 400 invalid; 401 unauthenticated; 403 forbidden; 404 not found; 409 duplicate/conflict; 500 unexpected.
- Centralized handling with `@RestControllerAdvice` + `@ExceptionHandler` (validation errors, not-found, duplicate, conflict, auth, file errors, generic 500).
- Content negotiation: request Content-Type decides input format; Accept header decides output. Support JSON and XML both ways.

---

## 8. Approved API catalogue (sections 12–15, 18)

Implement EXACTLY these. Do not add unapproved APIs.

### 8.1 Auth and profile
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register learner (always LEARNER, BCrypt) |
| POST | `/api/auth/login` | Login, return JWT |
| GET | `/api/users/me` | Current profile |
| PUT | `/api/users/me` | Update current profile |

### 8.2 Categories
| POST | `/api/admin/categories` | Create (ADMIN) |
| PUT | `/api/admin/categories/{categoryId}` | Update (ADMIN) |
| GET | `/api/categories` | List (all authenticated) |
| DELETE | `/api/admin/categories/{categoryId}` | Delete (ADMIN; 409 if courses reference it) |

### 8.3 Courses
| POST | `/api/admin/courses` | Create (DRAFT by default) |
| GET | `/api/courses/{courseId}` | Details |
| GET | `/api/courses` | List + search (paginated) |
| PUT | `/api/admin/courses/{courseId}` | Update (blocked if ARCHIVED) |
| PATCH | `/api/admin/courses/{courseId}/status` | Change status |
| DELETE | `/api/admin/courses/{courseId}` | Conditional delete (section 19) |

Search params (all optional): courseName, category, instructorName, keyword, page, size, sortBy, sortDirection.
- courseName / category / instructorName: partial, case-insensitive. Combined with AND.
- keyword: partial, case-insensitive across courseName, description, instructorName, category name (OR inside keyword).
- No filters = all courses visible to role. ADMIN: all statuses. LEARNER: PUBLISHED only.
- Pagination: page ≥ 0, size > 0, cap maximum size (e.g., 50). sortBy only from allowed whitelist (e.g., courseName, instructorName, durationSeconds, createdAt). Invalid sortDirection → 400 with clear message. Never sort by arbitrary entity field.

Status transitions (prompt rules): DRAFT ↔ PUBLISHED; PUBLISHED → ARCHIVED; ARCHIVED is permanent (cannot go back to PUBLISHED or DRAFT). Invalid transition → 409 (or 400; choose one and state it in the code comments and final report).

### 8.4 Videos
| GET | `/api/courses/{courseId}/videos` | List, sorted by sequenceNumber, not paginated |
| POST | `/api/admin/courses/{courseId}/videos/upload` | Multipart: videoTitle, videoFile, durationSeconds (manual) |
| POST | `/api/admin/courses/{courseId}/videos/link` | JSON/XML: videoTitle, videoUrl, durationSeconds |
| PUT | `/api/admin/courses/{courseId}/videos/{videoId}` | Update: videoTitle, sequenceNumber, durationSeconds |
| DELETE | `/api/admin/videos/{videoId}` | Delete (ADMIN) |

Rules:
- Only ADMIN manages videos. Archived course → cannot modify videos (409).
- Validate videoId belongs to courseId in path (404 if not).
- Sequence on add = max existing + 1 (0 if none → first is 1). Do not accept sequenceNumber on add (reject if sent, or ignore — state choice).
- Duration: manual, positive integer seconds, required for upload and link. Never auto-detect.
- Reorder: validate target 1..N; shift affected videos; avoid unique conflicts (use temporary value or two-step update inside a transaction).
- Recalculate course durationSeconds (sum) after add/update/delete.
- Recalculate affected enrollment progress after add or delete (section 15).
- Update must not change source type or URL.
- Use transactions for multi-row updates. Think about concurrent add/reorder: lock the course row (`@Lock(PESSIMISTIC_WRITE)` on a course query) before changing sequences so two requests cannot create the same sequence number. This is the only justified @Lock.

### 8.5 Video streaming
- Endpoint for UPLOAD videos only (define path, e.g., `GET /api/videos/{videoId}/stream`). Require JWT. Allowed: ADMIN, or enrolled learner of that course. Archived course: enrolled learners still allowed.
- Support HTTP Range (`Range: bytes=`) → 206 Partial Content with `Content-Range`, `Accept-Ranges: bytes`. No range → 200 full file. Invalid range → 416.
- EXTERNAL_LINK videos are NOT proxied (they are returned as URLs in the learning details).
- No complicated streaming infrastructure.

### 8.6 Course materials
| GET | `/api/courses/{courseId}/materials` | List |
| POST | `/api/admin/courses/{courseId}/materials/upload` | Multipart: materialTitle, file |
| GET | `/api/materials/{materialId}/download` | Binary download (JWT + access check) |
| DELETE | `/api/admin/materials/{materialId}` | Delete (ADMIN; blocked if course ARCHIVED) |

Rules:
- Only ADMIN upload/delete. ADMIN and enrolled learners can list/download. Learner list also needs enrollment.
- Download must check authorization every time (knowing the URL does not grant access). Return binary with correct Content-Type and Content-Disposition. Never return storage path.
- Delete: remove DB row and physical file. If DB delete ok and file delete fails → log and provide a retry/cleanup approach (simple: log WARN with path so admin can clean up; optionally a tiny cleanup method).
- Upload: if file save succeeds but DB save fails → delete the orphan file.

### 8.7 Course references
| GET | `/api/courses/{courseId}/references` | List (ADMIN, or enrolled learner) |
| POST | `/api/admin/courses/{courseId}/references` | Add (ADMIN; blocked if ARCHIVED) |
| PUT | `/api/admin/references/{referenceId}` | Update (ADMIN; blocked if ARCHIVED) |
| DELETE | `/api/admin/references/{referenceId}` | Delete (ADMIN; blocked if ARCHIVED) |

Validate URL: must parse and scheme must be http or https.

### 8.8 Enrollment and progress
| POST | `/api/courses/{courseId}/enroll` | Enroll (LEARNER only) |
| GET | `/api/enrollments/my` | My enrollments (not paginated) |
| GET | `/api/enrollments/{enrollmentId}/progress` | Progress (owner only) |
| PUT | `/api/enrollments/{enrollmentId}/videos/{videoId}/progress` | Update own video progress |
| GET | `/api/enrollments/{enrollmentId}` | Complete learning details (owner only) |

Enrollment rules:
- Only LEARNER enrolls. Course must be PUBLISHED (ARCHIVED → 409; DRAFT → 404 or 409, state choice). Duplicate → 409.
- Only enrollment owner views/updates (other users → 403; treat as not theirs).
- Progress update: video must belong to the enrollment's course (else 400/404). Upsert LessonProgress (one per enrollment+video). Set/clear completedAt. Then recalc enrollment.

Progress calculation (recalculate from LessonProgress records, never +/-):
- `completionPercentage = completedVideos / totalVideos * 100`, count-based, scale 2 (e.g., 2 of 4 = 50.00; 3 of 5 = 60.00).
- Zero videos → 0.00, status ENROLLED, do NOT mark COMPLETED.
- Status: 0 completed → ENROLLED; some → IN_PROGRESS; all → COMPLETED.
- All completed → set completedAt. Not all → clear completedAt.
- Video added → recalc (may move COMPLETED to IN_PROGRESS).
- Video deleted → delete its LessonProgress, recalc, update status, clear completedAt if no longer complete.
- Do all related changes in one transaction.

### 8.9 Complete learning details — `GET /api/enrollments/{enrollmentId}`
Response DTO (EnrollmentDetailsResponse):
- Enrollment: enrollmentId, enrolledAt, enrollmentStatus, completionPercentage, completedAt.
- Course: courseId, courseName, description, category, instructorName, difficultyLevel, language, durationSeconds, averageRating, totalRatings.
- videos[] sorted by sequenceNumber: videoId, videoTitle, videoSourceType, playbackUrl, sequenceNumber, durationSeconds, completed (false if no progress row), completedAt.
  - playbackUrl: for EXTERNAL_LINK = the URL; for UPLOAD = the protected stream endpoint path (never a storage path).
- materials[]: materialId, materialTitle, fileName, contentType, fileSize, protected download URL.
- references[]: referenceId, referenceTitle, referenceUrl.
- reviews: summary (averageRating, totalRatings), latest five reviews, myReview (or null).
- Owner-only (validate ownership first). Empty arrays, never null, for videos/materials/references. No physical paths.
- Binary download and video streaming stay separate endpoints.

---

## 9. Course list and details DTOs (section 17)

CourseListResponse: id, courseName, category (String name), instructorName, difficultyLevel, language, durationSeconds, status. NO rating/review counts.

CourseDetailsResponse: id, courseName, description, category (String), instructorName, difficultyLevel, language, durationSeconds, averageRating (BigDecimal), totalRatings (Long), totalVideos (Integer), totalMaterials (Integer), totalReferences (Integer), status, createdAt, updatedAt.

Never return JPA entities. Complete learning details use the separate DTO in 8.9.

---

## 10. Reviews (section 18)

| GET | `/api/courses/{courseId}/reviews` | Paginated, latest first |
| POST | `/api/courses/{courseId}/reviews` | Submit (enrolled LEARNER only) |
| PUT | `/api/reviews/{reviewId}` | Update own review only |

Rules:
- Only enrolled learners submit. Rating 1–5. Comment optional.
- One review per user per course → duplicate = 409.
- Update: only owner; keep submittedAt unchanged.
- No delete endpoint.
- Visibility: learners see reviews of PUBLISHED courses even before enrolling; enrolled learners see reviews of ARCHIVED courses; ADMIN sees all statuses. Non-enrolled learners on ARCHIVED → 404/403 (state choice).
- Average rating and total ratings computed from DB (AVG/COUNT query), not stored.
- Pagination: page, size with same validation as course search.

---

## 11. Conditional course deletion (section 19 — approved, do not change)

`DELETE /api/admin/courses/{courseId}`:
- **Has ≥1 enrollment** → do NOT delete. Return 409 with message telling admin to archive the course instead. Keep everything.
- **No enrollments** → physically delete: course, videos (+ their uploaded files), materials (+ their files), references, dependent LessonProgress if any. Do NOT delete the Category.
- Check enrollment existence before deleting. No unconditional CascadeType.REMOVE that bypasses this check.
- DB delete in a transaction. Physical file deletion handled after/safely; log failures.
- Consider FK constraints and unexpected dependent rows.

Individual video/material/reference deletion: allowed only when parent course is not ARCHIVED.

---

## 12. File storage and upload rules (section 13)

- Storage directory is configurable (`app.storage.root` in application.properties / env). Default outside source and build folders (e.g., `${user.home}/coursehub-storage`). NO hardcoded machine path.
- Separate subfolders: `videos/`, `materials/`.
- Store generated safe name (UUID + sanitized extension). Store relative key in DB, never client file path.
- Edge cases to handle: empty upload (400), unsupported format (400), max upload size (configure `spring.servlet.multipart.max-file-size` and `max-request-size`; map MaxUploadSizeExceededException to 400/413 with clear message), invalid/dangerous filename (strip path, reject `..`), duplicate filenames (UUID avoids clash), missing physical file on download/stream (404 with message, log), storage permission error (500 with safe message, log), DB save failure after file write (delete the file), path traversal (resolve and verify path starts with storage root).
- Client-provided path must never decide the physical location.

---

## 13. Application configuration (section 22)

`application.properties` (use `@Value` where configurable values are read — SRS requirement):
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password=${DB_PASSWORD}`.
- `spring.jpa.hibernate.ddl-auto=update` (dev), show-sql optional.
- `spring.servlet.multipart.max-file-size`, `max-request-size`.
- `app.storage.root=${APP_STORAGE_ROOT:${user.home}/coursehub-storage}` (or similar).
- `app.jwt.secret=${JWT_SECRET}`, `app.jwt.expiration-ms=86400000`.
- Optional `server.port`.
- No secrets in Java source. No extra profiles.

Read these in Java with `@Value`. Keep the DB password out of the file.

---

## 14. JSON and XML (SRS: produce and consume both)

- Add Jackson XML (Jackson 3 artifact managed by Boot 4 — verify version with `mvn dependency:tree`).
- Verify both: request JSON + Accept XML, request XML + Accept JSON, for at least one create and one get endpoint.
- Use Jackson annotations only where required (e.g., `@JacksonXmlRootElement` if needed for clean XML root names). Do not decorate every field.

---

## 15. Swagger / OpenAPI (SRS: all REST APIs documented)

- Springdoc UI at `/swagger-ui.html` (or `/swagger-ui/index.html`) and `/v3/api-docs`.
- `OpenApiConfig` with `@OpenAPIDefinition` and `@SecurityScheme` (HTTP bearer JWT).
- `@Tag` per controller. `@Operation` and `@ApiResponse` for main endpoints. `@Schema` on DTOs if helpful. Do not over-annotate.
- Every REST API must appear in Swagger.

---

## 16. Validation and pagination rules (section 21)

- DTO validation on every request input (`@Valid` / `@Validated`). Never rely only on frontend.
- Pagination only for: course catalogue and course reviews. Not for categories, videos, materials, references, my enrollments, details.
- Validate: page ≥ 0, size > 0, max size cap, allowed sort fields, valid sort direction.

---

## 17. Coding standards and rules (sections 6, 20, 25)

- Layers: Controller (thin, HTTP only) → Service (business logic, @Transactional where needed) → Repository → Entity. DTO and manual mapper at the edges.
- Constructor injection with `@RequiredArgsConstructor`.
- No unnecessary interfaces, factories, generic frameworks, or design patterns.
- Meaningful names, standard Java naming. No duplicated business logic. Keep service methods small.
- Entities: no `@Data`. Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` / `@Builder` where useful. Avoid recursive toString/equals/hashCode.
- Use `@Transactional` for multi-step DB updates.
- `@Query`, `@Param`, `@Modifying` only when needed.
- Use `@PreAuthorize` for role restrictions.
- Use `@Slf4j` where useful (file errors, delete failures).
- Explain important annotations briefly in the phase summary (what, why, where).
- Do not generate unnecessary classes. Do not create empty packages.
- Do not change approved entity fields or API contracts silently. Ask if unclear.
- Do not add features outside SRS/prompt without asking.
- If compilation fails, fix only the relevant issue.
- Never claim success without verification.

---

## 18. Phased work plan (section 24) — STOP after each phase for approval

### Phase 2 — Dependencies and configuration
1. Move `CoursehubApplication` to `com/coursehub/`, change package to `com.coursehub`. Delete `org/` folder and `CoursehubApplicationTests`.
2. pom.xml: remove the four `*-test` starters. Add `jackson-dataformat-xml` (Jackson 3, verify), JJWT api/impl/jackson (latest stable 0.12.x; verify and state version). Keep Boot-managed versions. Keep Lombok, springdoc, mysql connector, devtools.
3. application.properties: DB (password from env `DB_PASSWORD`), JPA, multipart limits, `app.storage.root`, `app.jwt.secret` (env `JWT_SECRET`), `app.jwt.expiration-ms=86400000`.
4. Verify: `mvn clean compile` passes. Report the verified versions.
- Stop. Wait for approval.

### Phase 3 — Entities, enums, repositories
1. Enums: UserRole, DifficultyLevel, CourseStatus, VideoSourceType, EnrollmentStatus.
2. Nine entities exactly per section 4 (names, types, lengths, unique constraints: users.email, users.phone_number, categories.category_name, enrollments(user_id,course_id), lesson_progress(enrollment_id,course_video_id), course_reviews(user_id,course_id), course_videos(course_id,sequence_number)).
3. Relationships per section 5.
4. Repositories: one per entity with needed query methods only (e.g., findByEmailIgnoreCase, existsByCategoryNameIgnoreCase, countByCourseId, findByCourseIdOrderBySequenceNumberAsc).
5. Verify: `mvn clean compile`; start app and confirm MySQL tables and FKs are created (ddl-auto=update). Confirm no JSON recursion (entities not serialized).
- Stop. Wait for approval.

### Phase 4 — Authentication and security
1. Register (LEARNER only, BCrypt, duplicate email 409 case-insensitive, duplicate phone 409).
2. Login → JWT (24h, HS256). Response DTO: token, tokenType, expiresIn, user summary.
3. SecurityConfig: public = register/login (+ swagger). Stateless. JwtAuthenticationFilter. Principal exposes userId.
4. CurrentUserService for principal → userId.
5. Profile: GET/PUT `/api/users/me` (update fullName, phoneNumber (unique), password optional). Never allow role change.
6. Verify manually: register, login, JWT, ADMIN vs LEARNER access, invalid/expired token (401), wrong role (403), duplicate email (409), duplicate phone (409). Admin account: create via DB seed or a documented manual step — ask user how to create the first ADMIN before coding it.
- Stop. Wait for approval.

### Phase 5 — Categories and courses
1. Category CRUD + duplicate (case-insensitive) 409 + delete blocked if referenced (409).
2. Course create (DRAFT), details, list/search (all filters, AND/OR rules, pagination, sort whitelist), update (blocked if ARCHIVED), status PATCH with transition rules, conditional delete (section 11).
3. Mappers for list/details DTOs. Course details counts (videos, materials, references).
4. Verify each item in section 23 for categories and courses.
- Stop. Wait for approval.

### Phase 6 — Videos and streaming
1. Upload (multipart, manual duration), link (manual duration), list, update (sequence reorder, title, duration; no source/url change), delete (close gap, delete progress, recalc).
2. Duration sum recalc on every change. Enrollment recalc after add/delete.
3. Locking for concurrent sequence changes (PESSIMISTIC_WRITE on course).
4. Streaming endpoint with Range → 206.
5. Verify: sequence generation, reorder, delete gap closing, duration recalc, archived blocked, range requests (curl with Range header).
- Stop. Wait for approval.

### Phase 7 — Materials and references
1. Material upload (PDF, DOCX, PPTX, XLSX, TXT), list, protected download (JWT + access check), delete (DB + file), archived blocked.
2. Reference CRUD with http/https validation, archived blocked for write.
3. Verify: upload, download as admin and enrolled learner, not-enrolled learner denied (403), unsupported type (400), empty file (400), path traversal rejected, orphan cleanup.
- Stop. Wait for approval.

### Phase 8 — Enrollment and progress
1. Enroll (LEARNER, PUBLISHED only, duplicate 409, archived 409).
2. My enrollments; progress (owner only); video progress update (upsert, completedAt set/clear, recalc).
3. Progress calculation per section 8.8, count-based, scale 2, zero-video rule.
4. Complete learning details (section 8.9).
5. Verify: 2/4 = 50.00, 3/5 = 60.00, 5/5 = 100 + COMPLETED + completedAt, zero-video course stays ENROLLED, unlearned video returns completed=false, other user gets 403.
- Stop. Wait for approval.

### Phase 9 — Reviews
1. Submit (enrolled LEARNER, rating 1–5, duplicate 409), list paginated latest first, update own (submittedAt unchanged), visibility rules.
2. Average rating and total ratings via DB query, included in course details and learning details.
3. Verify all items in section 10.
- Stop. Wait for approval.

### Phase 10 — Final verification and SRS audit
1. Re-check every SRS use case (21 items below) and every evaluation criterion (section 19 below). Mark each Done / Partial / Missing with evidence.
2. `mvn clean compile` and app startup. Swagger reachable. All endpoints listed.
3. Produce: setup instructions (MySQL DB name, env vars DB_PASSWORD, JWT_SECRET, APP_STORAGE_ROOT), run command, Swagger URL, endpoint summary, manual verification checklist (section 23), known limitations, simple project explanation for demo.
4. Do not hide incomplete features.

---

## 19. SRS evaluation criteria and use cases (from the SRS photos)

### 19.1 Evaluation criteria
- Spring RESTful APIs based on the use cases below.
- Technology: Spring Boot, Maven, Hibernate/JPA, MySQL.
- Proper coding standards for package, class, method, variable names.
- Proper layered architecture: Controller, Service, Repository, Model/DTO, Entity, Utility, Helper, Exception, Configuration.
- Read configurable/hard-coded values from application.properties using `@Value`.
- Application produces AND consumes both XML and JSON.
- One-to-Many entity associations implemented.
- Swagger/OpenAPI documentation for all REST APIs.
- Exception handling with `@RestControllerAdvice`.
- All APIs up and running during the presentation.

### 19.2 SRS use cases (must all be covered)
1. User Login
2. Register New User
3. Create New Course
4. Search Course Based on Course ID
5. Search Course Based on Course Name
6. Search Courses Based on Category
7. Search Courses Based on Instructor Name
8. Filter Courses Based on Keyword
9. Show Filtered Courses in Table View (backend returns filtered list; table is front-end later)
10. Admin can Add / Update / Delete Courses
11. Add Training Videos to a Course
12. Add / Update / Delete Course Categories
13. Upload / Delete Training Videos or Video Links
14. Upload / Download Course Materials
15. Add External Reference Links for a Course
16. Profile Settings where Users can Update their Details
17. Enroll User into a Course
18. View Enrolled Courses
19. Track Course Completion Status
20. Submit Course Rating and Feedback

Map each one to the endpoints in section 8 in the final audit (Phase 10).

---

## 20. Manual verification checklist (section 23 — must all be checked, report honestly)

1. `mvn clean compile`
2. App startup
3. MySQL connection
4. Tables and FKs created
5. Swagger UI
6. User registration
7. Login and JWT
8. ADMIN and LEARNER authorization
9. Invalid/expired token behavior
10. Duplicate email handling
11. Duplicate category handling
12. Course creation and update
13. Course search and filtering
14. Course status transitions
15. Course deletion without enrollments
16. Course deletion with enrollments → 409
17. Course archive behavior
18. Existing learner access to archived courses
19. Enrollment and duplicate enrollment
20. Progress calculations
21. Zero-video course behavior
22. Video addition and sequence generation
23. Video reorder and deletion
24. Course duration recalculation
25. Progress recalculation after video add/delete
26. Material upload and download
27. Download authorization
28. Video streaming and HTTP Range
29. Review submission and update
30. JSON request and response
31. XML request and response
32. Invalid request validation
33. Application logs

Do not claim a feature works until verified. If not verifiable, say why.

---

## 21. Open items to confirm with the user (ask one at a time; use the default if the user says "go ahead")

1. Status transitions: default = DRAFT↔PUBLISHED, PUBLISHED→ARCHIVED, ARCHIVED terminal. Invalid transition returns 409.
2. Learner on DRAFT course (GET details / enroll): default = 404 for details, 409 for enroll.
3. Learner on ARCHIVED course not enrolled: default = 404 for details, 403 for review submit.
4. Sending `sequenceNumber` on add: default = reject with 400.
5. First ADMIN account creation: default = seed via SQL/manual step documented in README (no public admin registration).
6. Max upload size: default = 500 MB videos, 50 MB materials (confirm).
7. Storage root default: `${user.home}/coursehub-storage`, overridable by env `APP_STORAGE_ROOT`.

---

## 22. Final deliverables (section 26)

Backend source, Maven config, entities and relationships, REST APIs, JWT auth and authorization, DTOs, validation, `@RestControllerAdvice` handling, file upload/download, video streaming, JSON and XML, Swagger, DB setup, run instructions, Swagger URL, endpoint summary, manual verification checklist, SRS mapping (section 19), known limitations, simple demo explanation.

---

## 23. Start instructions for Sonnet

1. Read this file fully and the master prompt. Do not change any rule without asking.
2. Start with **Phase 2 only**. Do not start Phase 3 until the user approves Phase 2.
3. Use the user's decisions in section 0 (com.coursehub, unique phone, manual video duration, no FFprobe).
4. Report after each phase: files created/changed, versions verified, what you verified, what you did not verify, and wait.

---

## 24. Sonnet review of this plan — corrections and additions (these override earlier text)

Overall the plan is correct and covers the master prompt and SRS. These are the gaps/fixes found:

**Phase 2 status: DONE and compile-verified.** Resolved versions: jackson-dataformat-xml 3.1.7 (Jackson 3, Boot-managed), JJWT 0.12.6, springdoc 3.2.0, Boot 4.1.2. Not yet verified: app startup, MySQL connection, Swagger (MySQL is not running on this machine's port 3306 right now; env vars `DB_PASSWORD` and `JWT_SECRET` are not set). Add `?createDatabaseIfNotExist=true` to the JDBC URL so `coursehub_db` is created automatically.

**Dependency note.** `jjwt-jackson` brings Jackson 2 `databind` (2.21.7). springdoc 3.2.0 already brings Jackson 2 modules, so this is acceptable; JJWT uses its own internal mapper and does not touch Spring's Jackson 3 setup. Keep it. For Jackson XML annotations use `tools.jackson.dataformat.xml.annotation.*`; `com.fasterxml.jackson.annotation.*` (annotations) is unchanged in Jackson 3.

**Corrections to add to the build:**
1. **401 vs 403 (Phase 4).** With no token, Spring Security's default returns 403. Add a custom `AuthenticationEntryPoint` (401, JSON `ErrorResponse`) and `AccessDeniedHandler` (403, JSON `ErrorResponse`) in `SecurityConfig`. Also add a URL rule `/api/admin/**` → ADMIN as a second layer next to `@PreAuthorize`.
2. **Paging DTO (Phase 5/9).** Do not return Spring `Page` from controllers (poor JSON/XML shape). Use one generic `PagedResponse<T>` DTO: content, page, size, totalElements, totalPages.
3. **XML shape.** List responses must serialize cleanly in XML (wrapper DTO or `@JacksonXmlElementWrapper`). Check what format is returned when no `Accept` header is sent; JSON must be the default. Verify JSON and XML on one POST and one GET in Phase 4/5 before moving on.
4. **Upload size per type.** `spring.servlet.multipart` is one global limit (500MB). Enforce 50MB for materials and 500MB for videos in the service and return a clear 400.
5. **Reorder algorithm (Phase 6).** Lock the course row (`@Lock(PESSIMISTIC_WRITE)` on `findByIdForUpdate`). Load videos by sequence, reorder in memory, `saveAllAndFlush` once with temporary negative sequence values, then assign final 1..N and flush. This avoids the unique `(course_id, sequence_number)` violation.
6. **Average rating.** Compute with `AVG`/`COUNT` queries; round to 2 decimals HALF_UP; with no reviews return averageRating `0.00` and totalRatings `0` (never null).
7. **Email.** Trim and store lowercase; look up by lowercase.
8. **Streaming path.** `GET /api/videos/{videoId}/stream` is not listed in the approved catalogue but is required by prompt section 13.3, so it is added; tell the user in the Phase 6 summary.
9. **`@Value` (SRS criterion).** Read configuration through `@Value` in `JwtService`, `FileStorageService` and the admin seeder; do not use `@ConfigurationProperties` for these.

**Open items — recommended defaults (ask the user in the relevant phase):**
- Status transitions: allow DRAFT→PUBLISHED, PUBLISHED→ARCHIVED, DRAFT→ARCHIVED; allow PUBLISHED→DRAFT only if the course has no enrollments (else 409); ARCHIVED terminal (409).
- First ADMIN: recommend a small startup `CommandLineRunner` that creates one ADMIN from `app.admin.email` / `app.admin.password` (env vars) if none exists. Ask user before adding (outside the approved class list).
- DB password: user has not decided yet between plain text in `application.properties` and env var `DB_PASSWORD`. Prompt says no hardcoded credentials, so env var is the default; ask before changing.

---

## 25. Local test accounts (created during Phase 4 verification, local dev DB only)

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@coursehub.com | Admin@12345 |
| LEARNER | learner1@test.com | Learner@123 |
| LEARNER | xml@test.com | Learner@123 |

The admin is created at startup by `config/DataInitializer` (reads `app.admin.*` from application.properties) because public registration can only create LEARNER users.
