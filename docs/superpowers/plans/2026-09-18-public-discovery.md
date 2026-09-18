# Public Help Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `/` marketing landing with a public help-discovery page (multi-pin map + open needs list, pin filter + free-text, need drawer with link to `/r/:slug`), backed by `GET /api/public/discovery`.

**Architecture:** One unauthenticated discovery endpoint returns ACTIVE relief-request pins and OPEN/PARTIALLY_COVERED needs. Frontend loads once, filters client-side, reuses `PublicReliefSplit` layout and adds `DiscoveryMap` + drawer. Existing slug public page stays for full detail/offers.

**Tech Stack:** Spring Boot 4 / Java 25, JPA, OpenAPI YAML + Orval, React 19, Vite 6, Mantine 9, react-leaflet, i18next, Vitest, Playwright, Yarn 4

**Spec:** `docs/superpowers/specs/2026-09-18-public-discovery-design.md`

## Global Constraints

- English only in code/comments/commits; UI strings via i18n (`en.json` + `de.json`).
- JSDoc on every new function; zero unused imports/vars.
- No entity types in API responses — DTOs only.
- Discovery needs: status `OPEN` or `PARTIALLY_COVERED` only; points: `ReliefRequestStatus.ACTIVE` only.
- Package manager: **Yarn 4** only (`yarn`, not npm).
- If `git status` fails (no repo), **skip commit steps** and continue.
- Verify each backend task with Maven tests; frontend with `yarn build` / Vitest / Playwright as listed.

---

## File structure (locked)

| Path | Responsibility |
|------|----------------|
| `needrelay-backend/.../web/dto/DiscoveryPointResponse.java` | Map pin DTO |
| `needrelay-backend/.../web/dto/DiscoveryNeedResponse.java` | List-row need DTO |
| `needrelay-backend/.../web/dto/PublicDiscoveryResponse.java` | Wrapper `{ points, needs }` |
| `needrelay-backend/.../repository/ReliefRequestRepository.java` | ACTIVE + organizer fetch |
| `needrelay-backend/.../repository/NeedRepository.java` | Discoverable needs query |
| `needrelay-backend/.../service/PublicDiscoveryService.java` | Assemble discovery payload |
| `needrelay-backend/.../web/PublicController.java` | `GET /api/public/discovery` |
| `needrelay-backend/.../service/PublicDiscoveryServiceIT.java` | Integration test |
| `needrelay-frontend/openapi/openapi.yaml` | Document discovery schemas + path |
| `needrelay-frontend/src/api/generated/**` | Orval output (regenerated) |
| `needrelay-frontend/src/lib/discoveryFilter.ts` | Pin + text filter helpers |
| `needrelay-frontend/src/lib/discoveryFilter.test.ts` | Vitest |
| `needrelay-frontend/src/components/map/DiscoveryMap.tsx` | Multi-marker Leaflet map |
| `needrelay-frontend/src/pages/LandingPage.tsx` | Becomes discovery UI (keep filename or rename — prefer rename to `DiscoveryPage.tsx` + update `App.tsx`) |
| `needrelay-frontend/src/locales/en.json`, `de.json` | `discovery.*` keys; remove unused `landing.*` |
| `needrelay-frontend/e2e/landing.spec.ts` | Replace with discovery e2e (rename to `discovery.spec.ts`) |

---

### Task 1: Backend discovery DTOs, repos, service, endpoint + IT

**Files:**
- Create: `needrelay-backend/src/main/java/com/yagci/needrelay/web/dto/DiscoveryPointResponse.java`
- Create: `needrelay-backend/src/main/java/com/yagci/needrelay/web/dto/DiscoveryNeedResponse.java`
- Create: `needrelay-backend/src/main/java/com/yagci/needrelay/web/dto/PublicDiscoveryResponse.java`
- Create: `needrelay-backend/src/main/java/com/yagci/needrelay/service/PublicDiscoveryService.java`
- Create: `needrelay-backend/src/test/java/com/yagci/needrelay/service/PublicDiscoveryServiceIT.java`
- Modify: `needrelay-backend/src/main/java/com/yagci/needrelay/repository/ReliefRequestRepository.java`
- Modify: `needrelay-backend/src/main/java/com/yagci/needrelay/repository/NeedRepository.java`
- Modify: `needrelay-backend/src/main/java/com/yagci/needrelay/web/PublicController.java`

**Interfaces:**
- Consumes: `ReliefRequest`, `Need`, `Organizer`, existing enums
- Produces: `PublicDiscoveryService.getDiscovery() → PublicDiscoveryResponse`; `GET /api/public/discovery`

- [ ] **Step 1: Write failing IT**

Create `PublicDiscoveryServiceIT.java`:

```java
package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Need;
import com.yagci.needrelay.domain.NeedCategory;
import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.domain.ReliefRequest;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.OrganizerRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PublicDiscoveryServiceIT {

	@Autowired
	private PublicDiscoveryService publicDiscoveryService;

	@Autowired
	private OrganizerRepository organizerRepository;

	@Autowired
	private ReliefRequestRepository reliefRequestRepository;

	@Autowired
	private NeedRepository needRepository;

	@BeforeEach
	void setUp() {
		needRepository.deleteAll();
		reliefRequestRepository.deleteAll();
		organizerRepository.deleteAll();

		Organizer organizer = new Organizer();
		organizer.setEmail("disc@example.com");
		organizer.setPasswordHash("hash");
		organizer.setDisplayName("Aid Org");
		organizer.setRole(OrganizerRole.ORGANIZER);
		organizerRepository.save(organizer);

		ReliefRequest active = new ReliefRequest();
		active.setOrganizer(organizer);
		active.setTitle("Active Camp");
		active.setDescription("d");
		active.setLocationLabel("Aleppo North");
		active.setLatitude(36.2);
		active.setLongitude(37.1);
		active.setPublicSlug("active-camp");
		active.setStatus(ReliefRequestStatus.ACTIVE);
		reliefRequestRepository.save(active);

		ReliefRequest archived = new ReliefRequest();
		archived.setOrganizer(organizer);
		archived.setTitle("Archived Camp");
		archived.setDescription("d");
		archived.setLocationLabel("Elsewhere");
		archived.setLatitude(1);
		archived.setLongitude(2);
		archived.setPublicSlug("archived-camp");
		archived.setStatus(ReliefRequestStatus.ARCHIVED);
		reliefRequestRepository.save(archived);

		Need openNeed = new Need();
		openNeed.setReliefRequest(active);
		openNeed.setTitle("Water");
		openNeed.setCategory(NeedCategory.WATER);
		openNeed.setQuantityRequired(new BigDecimal("100"));
		openNeed.setQuantityOffered(BigDecimal.ZERO);
		openNeed.setUnit("L");
		openNeed.setPriority(NeedPriority.CRITICAL);
		openNeed.setStatus(NeedStatus.OPEN);
		needRepository.save(openNeed);

		Need covered = new Need();
		covered.setReliefRequest(active);
		covered.setTitle("Blankets");
		covered.setCategory(NeedCategory.SHELTER);
		covered.setQuantityRequired(new BigDecimal("10"));
		covered.setQuantityOffered(new BigDecimal("10"));
		covered.setUnit("pcs");
		covered.setPriority(NeedPriority.LOW);
		covered.setStatus(NeedStatus.COVERED);
		needRepository.save(covered);

		Need closedNeed = new Need();
		closedNeed.setReliefRequest(active);
		closedNeed.setTitle("Old");
		closedNeed.setCategory(NeedCategory.OTHER);
		closedNeed.setQuantityRequired(new BigDecimal("1"));
		closedNeed.setQuantityOffered(BigDecimal.ZERO);
		closedNeed.setUnit("x");
		closedNeed.setPriority(NeedPriority.NORMAL);
		closedNeed.setStatus(NeedStatus.CLOSED);
		needRepository.save(closedNeed);

		Need onArchivedRequest = new Need();
		onArchivedRequest.setReliefRequest(archived);
		onArchivedRequest.setTitle("Should hide");
		onArchivedRequest.setCategory(NeedCategory.FOOD);
		onArchivedRequest.setQuantityRequired(new BigDecimal("5"));
		onArchivedRequest.setQuantityOffered(BigDecimal.ZERO);
		onArchivedRequest.setUnit("kg");
		onArchivedRequest.setPriority(NeedPriority.HIGH);
		onArchivedRequest.setStatus(NeedStatus.OPEN);
		needRepository.save(onArchivedRequest);
	}

	@Test
	void returnsOnlyActivePointsAndOpenOrPartialNeeds() {
		var result = publicDiscoveryService.getDiscovery();
		assertThat(result.points()).hasSize(1);
		assertThat(result.points().getFirst().publicSlug()).isEqualTo("active-camp");
		assertThat(result.points().getFirst().locationLabel()).isEqualTo("Aleppo North");
		assertThat(result.needs()).hasSize(1);
		assertThat(result.needs().getFirst().title()).isEqualTo("Water");
		assertThat(result.needs().getFirst().organizationName()).isEqualTo("Aid Org");
		assertThat(result.needs().getFirst().publicSlug()).isEqualTo("active-camp");
	}
}
```

- [ ] **Step 2: Run IT — expect fail (missing bean / compile)**

```bash
cd needrelay-backend
./mvnw -q -Dtest=PublicDiscoveryServiceIT test
```

Expected: FAIL (class/bean not found or compile error).

- [ ] **Step 3: Add DTOs**

`DiscoveryPointResponse.java`:

```java
package com.yagci.needrelay.web.dto;

import java.util.UUID;

/**
 * Map pin for an ACTIVE public relief request.
 *
 * @param id request id
 * @param title title
 * @param locationLabel location label
 * @param latitude latitude
 * @param longitude longitude
 * @param publicSlug public slug
 */
public record DiscoveryPointResponse(
		UUID id,
		String title,
		String locationLabel,
		double latitude,
		double longitude,
		String publicSlug
) {
}
```

`DiscoveryNeedResponse.java`:

```java
package com.yagci.needrelay.web.dto;

import com.yagci.needrelay.domain.NeedPriority;
import com.yagci.needrelay.domain.NeedStatus;

import java.util.UUID;

/**
 * Discoverable need row for the public help list.
 *
 * @param id need id
 * @param title title
 * @param priority priority
 * @param status status
 * @param requestId parent request id
 * @param publicSlug parent public slug
 * @param locationLabel parent location label
 * @param organizationName owning organization display name
 */
public record DiscoveryNeedResponse(
		UUID id,
		String title,
		NeedPriority priority,
		NeedStatus status,
		UUID requestId,
		String publicSlug,
		String locationLabel,
		String organizationName
) {
}
```

`PublicDiscoveryResponse.java`:

```java
package com.yagci.needrelay.web.dto;

import java.util.List;

/**
 * Public discovery payload for map pins and open needs.
 *
 * @param points ACTIVE relief request pins
 * @param needs open/partial needs on those requests
 */
public record PublicDiscoveryResponse(
		List<DiscoveryPointResponse> points,
		List<DiscoveryNeedResponse> needs
) {
}
```

- [ ] **Step 4: Repository methods**

In `ReliefRequestRepository`:

```java
/**
 * Lists ACTIVE relief requests with organizer loaded.
 *
 * @param status request status
 * @return requests
 */
@Query("select r from ReliefRequest r join fetch r.organizer where r.status = :status order by r.createdAt desc")
List<ReliefRequest> findByStatusWithOrganizer(@Param("status") ReliefRequestStatus status);
```

Add imports for `ReliefRequestStatus`.

In `NeedRepository`:

```java
/**
 * Lists discoverable needs on ACTIVE requests.
 *
 * @param requestStatus ACTIVE
 * @param needStatuses OPEN and PARTIALLY_COVERED
 * @return needs with request + organizer loaded
 */
@Query("""
		select n from Need n
		join fetch n.reliefRequest r
		join fetch r.organizer
		where r.status = :requestStatus
		  and n.status in :needStatuses
		order by n.priority asc, n.createdAt asc
		""")
List<Need> findDiscoverable(
		@Param("requestStatus") ReliefRequestStatus requestStatus,
		@Param("needStatuses") List<NeedStatus> needStatuses);
```

Add import for `ReliefRequestStatus`.

- [ ] **Step 5: Service + controller**

`PublicDiscoveryService.java`:

```java
package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.NeedStatus;
import com.yagci.needrelay.domain.ReliefRequestStatus;
import com.yagci.needrelay.repository.NeedRepository;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import com.yagci.needrelay.web.dto.DiscoveryNeedResponse;
import com.yagci.needrelay.web.dto.DiscoveryPointResponse;
import com.yagci.needrelay.web.dto.PublicDiscoveryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Builds the unauthenticated public discovery feed.
 */
@Service
public class PublicDiscoveryService {

	private static final List<NeedStatus> DISCOVERABLE = List.of(
			NeedStatus.OPEN,
			NeedStatus.PARTIALLY_COVERED);

	private final ReliefRequestRepository reliefRequestRepository;
	private final NeedRepository needRepository;

	/**
	 * @param reliefRequestRepository relief requests
	 * @param needRepository needs
	 */
	public PublicDiscoveryService(
			ReliefRequestRepository reliefRequestRepository,
			NeedRepository needRepository) {
		this.reliefRequestRepository = reliefRequestRepository;
		this.needRepository = needRepository;
	}

	/**
	 * Returns ACTIVE pins and discoverable needs.
	 *
	 * @return discovery payload
	 */
	@Transactional(readOnly = true)
	public PublicDiscoveryResponse getDiscovery() {
		var points = reliefRequestRepository
				.findByStatusWithOrganizer(ReliefRequestStatus.ACTIVE)
				.stream()
				.map(r -> new DiscoveryPointResponse(
						r.getId(),
						r.getTitle(),
						r.getLocationLabel(),
						r.getLatitude(),
						r.getLongitude(),
						r.getPublicSlug()))
				.toList();

		var needs = needRepository
				.findDiscoverable(ReliefRequestStatus.ACTIVE, DISCOVERABLE)
				.stream()
				.map(n -> {
					var r = n.getReliefRequest();
					return new DiscoveryNeedResponse(
							n.getId(),
							n.getTitle(),
							n.getPriority(),
							n.getStatus(),
							r.getId(),
							r.getPublicSlug(),
							r.getLocationLabel(),
							r.getOrganizer().getDisplayName());
				})
				.toList();

		return new PublicDiscoveryResponse(points, needs);
	}
}
```

In `PublicController`, inject `PublicDiscoveryService` and add:

```java
/**
 * Returns ACTIVE help points and discoverable needs for the public map/list.
 *
 * @return discovery payload
 */
@GetMapping("/discovery")
@Operation(summary = "Public help discovery feed")
@ApiResponse(responseCode = "200", description = "Discovery payload")
public PublicDiscoveryResponse discovery() {
	return publicDiscoveryService.getDiscovery();
}
```

Update constructor accordingly.

- [ ] **Step 6: Run IT — expect pass**

```bash
cd needrelay-backend
./mvnw -q -Dtest=PublicDiscoveryServiceIT test
```

Expected: PASS.

- [ ] **Step 7: Commit** (skip if no git repo)

```bash
git add needrelay-backend/src/main/java/com/yagci/needrelay/web/dto/Discovery*.java \
  needrelay-backend/src/main/java/com/yagci/needrelay/web/dto/PublicDiscoveryResponse.java \
  needrelay-backend/src/main/java/com/yagci/needrelay/service/PublicDiscoveryService.java \
  needrelay-backend/src/main/java/com/yagci/needrelay/repository/*.java \
  needrelay-backend/src/main/java/com/yagci/needrelay/web/PublicController.java \
  needrelay-backend/src/test/java/com/yagci/needrelay/service/PublicDiscoveryServiceIT.java
git commit -m "feat: add public discovery API for map and needs"
```

---

### Task 2: OpenAPI + Orval client

**Files:**
- Modify: `needrelay-frontend/openapi/openapi.yaml`
- Regenerate: `needrelay-frontend/src/api/generated/**`

**Interfaces:**
- Consumes: Task 1 DTOs / path
- Produces: `publicApi.getPublicDiscovery()` (orval `operationId: getPublicDiscovery`)

- [ ] **Step 1: Add schemas to `openapi.yaml` under `components.schemas`**

```yaml
    DiscoveryPointResponse:
      type: object
      required: [id, title, locationLabel, latitude, longitude, publicSlug]
      properties:
        id: { type: string, format: uuid }
        title: { type: string }
        locationLabel: { type: string }
        latitude: { type: number, format: double }
        longitude: { type: number, format: double }
        publicSlug: { type: string }
    DiscoveryNeedResponse:
      type: object
      required: [id, title, priority, status, requestId, publicSlug, locationLabel, organizationName]
      properties:
        id: { type: string, format: uuid }
        title: { type: string }
        priority: { $ref: '#/components/schemas/NeedPriority' }
        status: { $ref: '#/components/schemas/NeedStatus' }
        requestId: { type: string, format: uuid }
        publicSlug: { type: string }
        locationLabel: { type: string }
        organizationName: { type: string }
    PublicDiscoveryResponse:
      type: object
      required: [points, needs]
      properties:
        points:
          type: array
          items: { $ref: '#/components/schemas/DiscoveryPointResponse' }
        needs:
          type: array
          items: { $ref: '#/components/schemas/DiscoveryNeedResponse' }
```

- [ ] **Step 2: Add path before `/api/public/relief-requests/{slug}`**

```yaml
  /api/public/discovery:
    get:
      tags: [public]
      operationId: getPublicDiscovery
      responses:
        '200':
          description: Discovery payload
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PublicDiscoveryResponse'
```

- [ ] **Step 3: Regenerate client**

```bash
cd needrelay-frontend
yarn generate:api
```

Expected: `getPublicDiscovery` appears in `src/api/generated/public/public.ts`.

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add needrelay-frontend/openapi/openapi.yaml needrelay-frontend/src/api/generated
git commit -m "feat: generate Orval client for public discovery"
```

---

### Task 3: Client filter helpers + Vitest

**Files:**
- Create: `needrelay-frontend/src/lib/discoveryFilter.ts`
- Create: `needrelay-frontend/src/lib/discoveryFilter.test.ts`

**Interfaces:**
- Consumes: `DiscoveryNeedResponse` from generated models
- Produces: `filterDiscoveryNeeds(needs, { requestId?, query? })`

- [ ] **Step 1: Write failing tests**

```ts
import { describe, expect, it } from 'vitest';
import { NeedPriority, NeedStatus } from '../api/generated/models';
import type { DiscoveryNeedResponse } from '../api/generated/models';
import { filterDiscoveryNeeds } from './discoveryFilter';

const base: DiscoveryNeedResponse = {
  id: 'n1',
  title: 'Water tanks',
  priority: NeedPriority.CRITICAL,
  status: NeedStatus.OPEN,
  requestId: 'r1',
  publicSlug: 'camp-a',
  locationLabel: 'Aleppo North',
  organizationName: 'Aid Org',
};

describe('filterDiscoveryNeeds', () => {
  it('filters by selected request id', () => {
    const other = { ...base, id: 'n2', requestId: 'r2', title: 'Food' };
    expect(filterDiscoveryNeeds([base, other], { requestId: 'r1' })).toEqual([base]);
  });

  it('filters by free text on title and locationLabel', () => {
    const other = {
      ...base,
      id: 'n2',
      title: 'Blankets',
      locationLabel: 'Damascus',
    };
    expect(filterDiscoveryNeeds([base, other], { query: 'aleppo' })).toEqual([base]);
    expect(filterDiscoveryNeeds([base, other], { query: 'blank' })).toEqual([other]);
  });

  it('combines pin and text filters', () => {
    const samePin = { ...base, id: 'n2', title: 'Food', locationLabel: 'Aleppo North' };
    expect(
      filterDiscoveryNeeds([base, samePin], { requestId: 'r1', query: 'water' }),
    ).toEqual([base]);
  });
});
```

- [ ] **Step 2: Run — expect fail**

```bash
cd needrelay-frontend
yarn vitest run src/lib/discoveryFilter.test.ts
```

Expected: FAIL (module not found).

- [ ] **Step 3: Implement**

```ts
import type { DiscoveryNeedResponse } from '../api/generated/models';

export interface DiscoveryFilter {
  requestId?: string | null;
  query?: string;
}

/**
 * Filters discovery needs by optional pin selection and free-text query.
 *
 * @param needs discovery needs
 * @param filter requestId and/or query
 * @returns filtered needs (same order)
 */
export function filterDiscoveryNeeds(
  needs: DiscoveryNeedResponse[],
  filter: DiscoveryFilter,
): DiscoveryNeedResponse[] {
  const q = filter.query?.trim().toLowerCase() ?? '';
  return needs.filter((need) => {
    if (filter.requestId && need.requestId !== filter.requestId) {
      return false;
    }
    if (!q) {
      return true;
    }
    return (
      need.title.toLowerCase().includes(q) ||
      need.locationLabel.toLowerCase().includes(q)
    );
  });
}
```

- [ ] **Step 4: Run — expect pass**

```bash
cd needrelay-frontend
yarn vitest run src/lib/discoveryFilter.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit** (skip if no git)

```bash
git add needrelay-frontend/src/lib/discoveryFilter.ts needrelay-frontend/src/lib/discoveryFilter.test.ts
git commit -m "feat: add discovery need filter helpers"
```

---

### Task 4: DiscoveryMap multi-marker component

**Files:**
- Create: `needrelay-frontend/src/components/map/DiscoveryMap.tsx`

**Interfaces:**
- Consumes: pin `{ id, latitude, longitude, title }[]`, `selectedId`, `onSelect(id | null)`
- Produces: clickable markers; selected pin highlighted via opacity or larger icon if easy, else title tooltip only

- [ ] **Step 1: Implement `DiscoveryMap.tsx`**

Follow Leaflet icon fix pattern from `LocationMap.tsx`. Approximate implementation:

```tsx
import { Box, Paper } from '@mantine/core';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import 'leaflet/dist/leaflet.css';
import { useEffect, useMemo } from 'react';
import { MapContainer, Marker, TileLayer, useMap } from 'react-leaflet';

// same L.Icon.Default.mergeOptions fix as LocationMap

export interface DiscoveryMapPoint {
  id: string;
  latitude: number;
  longitude: number;
  title: string;
}

export interface DiscoveryMapProps {
  points: DiscoveryMapPoint[];
  selectedId?: string | null;
  onSelect: (id: string | null) => void;
  height?: number;
}

/**
 * Fits map bounds to all points (or a default center when empty).
 */
function FitBounds({ points }: { points: DiscoveryMapPoint[] }) {
  const map = useMap();
  useEffect(() => {
    if (points.length === 0) {
      map.setView([33.5, 36.3], 6);
      return;
    }
    if (points.length === 1) {
      map.setView([points[0].latitude, points[0].longitude], 12);
      return;
    }
    const bounds = L.latLngBounds(
      points.map((p) => [p.latitude, p.longitude] as [number, number]),
    );
    map.fitBounds(bounds, { padding: [24, 24] });
  }, [map, points]);
  return null;
}

/**
 * Multi-marker map for public help-point discovery.
 *
 * @param props points, selection, and click handler
 * @returns map UI
 */
export function DiscoveryMap({
  points,
  selectedId,
  onSelect,
  height = 360,
}: DiscoveryMapProps) {
  const center = useMemo<[number, number]>(() => {
    if (points[0]) {
      return [points[0].latitude, points[0].longitude];
    }
    return [33.5, 36.3];
  }, [points]);

  return (
    <Paper withBorder radius="md" style={{ overflow: 'hidden' }} data-testid="discovery-map">
      <Box style={{ height }}>
        <MapContainer center={center} zoom={6} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FitBounds points={points} />
          {points.map((point) => (
            <Marker
              key={point.id}
              position={[point.latitude, point.longitude]}
              opacity={selectedId && selectedId !== point.id ? 0.45 : 1}
              eventHandlers={{
                click: () => {
                  onSelect(selectedId === point.id ? null : point.id);
                },
              }}
              title={point.title}
            />
          ))}
        </MapContainer>
      </Box>
    </Paper>
  );
}
```

- [ ] **Step 2: Typecheck via build**

```bash
cd needrelay-frontend
yarn build
```

Expected: PASS (page not wired yet is fine if unused — if lint forbids unused files, wait until Task 5 wires it; prefer wiring in Task 5 same session if eslint fails).

- [ ] **Step 3: Commit** (skip if no git)

```bash
git add needrelay-frontend/src/components/map/DiscoveryMap.tsx
git commit -m "feat: add multi-marker DiscoveryMap"
```

---

### Task 5: Discovery page UI (replace landing)

**Files:**
- Create: `needrelay-frontend/src/pages/DiscoveryPage.tsx`
- Modify: `needrelay-frontend/src/App.tsx` (index route → `DiscoveryPage`)
- Delete or leave unused: `needrelay-frontend/src/pages/LandingPage.tsx` (delete after switch)
- Modify: `needrelay-frontend/src/locales/en.json`, `de.json`

**Interfaces:**
- Consumes: `publicApi.getPublicDiscovery`, `filterDiscoveryNeeds`, `DiscoveryMap`, `PublicReliefSplit`, Mantine `Drawer`
- Produces: `/` discovery UX per spec

- [ ] **Step 1: Add i18n keys**

`en.json` — replace `landing` block with:

```json
  "discovery": {
    "title": "Find where help is needed",
    "searchPlaceholder": "Search by place or need…",
    "allPoints": "All help points",
    "emptyPoints": "No active help points yet.",
    "emptyNeeds": "No open needs match your filters.",
    "loadError": "Could not load help areas.",
    "retry": "Retry",
    "openRequest": "Open help page",
    "selectedPoint": "Filtered to selected help point",
    "org": "Organization",
    "location": "Location",
    "priority": "Priority"
  },
```

Mirror in `de.json` (German UI strings). Remove unused `landing.*` keys.

- [ ] **Step 2: Implement `DiscoveryPage.tsx`**

Core behavior:

1. `useEffect` load `publicApi.getPublicDiscovery()` into state (`points`, `needs`, `loading`, `error`).
2. State: `selectedRequestId: string | null`, `query: string`, `drawerNeed: DiscoveryNeedResponse | null`.
3. `visibleNeeds = filterDiscoveryNeeds(needs, { requestId: selectedRequestId, query })` then sort by `priorityRank` from `urgency.ts` (adapt or map priority).
4. Layout: short title + search `TextInput` + optional “All help points” button when pin selected; `PublicReliefSplit` with `DiscoveryMap` + need list (`SurfaceCard` or simple stack rows with `data-testid="discovery-need-{id}"`).
5. Need click opens `Drawer` with title, priority, location, org, `Button component={Link} to={`/r/${need.publicSlug}`}`.
6. Empty/error states per i18n keys; `data-testid="discovery-page"`, `discovery-search`, `discovery-retry`, `discovery-drawer`, `discovery-open-request`.

- [ ] **Step 3: Wire route; delete `LandingPage.tsx`**

In `App.tsx`:

```tsx
import { DiscoveryPage } from './pages/DiscoveryPage';
// ...
<Route index element={<DiscoveryPage />} />
```

Delete `LandingPage.tsx`.

- [ ] **Step 4: Build**

```bash
cd needrelay-frontend
yarn build
```

Expected: PASS.

- [ ] **Step 5: Commit** (skip if no git)

```bash
git add needrelay-frontend/src/pages/DiscoveryPage.tsx needrelay-frontend/src/App.tsx \
  needrelay-frontend/src/locales/en.json needrelay-frontend/src/locales/de.json
git rm needrelay-frontend/src/pages/LandingPage.tsx
git commit -m "feat: replace landing with public help discovery page"
```

---

### Task 6: Playwright e2e for discovery

**Files:**
- Create: `needrelay-frontend/e2e/discovery.spec.ts`
- Delete: `needrelay-frontend/e2e/landing.spec.ts`

**Interfaces:**
- Consumes: discovery page testids; mocks `GET /api/public/discovery`

- [ ] **Step 1: Write e2e with API mock**

```ts
import { expect, test } from '@playwright/test';

const payload = {
  points: [
    {
      id: '11111111-1111-1111-1111-111111111111',
      title: 'Camp A',
      locationLabel: 'Aleppo North',
      latitude: 36.2,
      longitude: 37.1,
      publicSlug: 'camp-a',
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      title: 'Camp B',
      locationLabel: 'Damascus',
      latitude: 33.5,
      longitude: 36.3,
      publicSlug: 'camp-b',
    },
  ],
  needs: [
    {
      id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      title: 'Water tanks',
      priority: 'CRITICAL',
      status: 'OPEN',
      requestId: '11111111-1111-1111-1111-111111111111',
      publicSlug: 'camp-a',
      locationLabel: 'Aleppo North',
      organizationName: 'Aid Org',
    },
    {
      id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      title: 'Blankets',
      priority: 'HIGH',
      status: 'OPEN',
      requestId: '22222222-2222-2222-2222-222222222222',
      publicSlug: 'camp-b',
      locationLabel: 'Damascus',
      organizationName: 'Other Org',
    },
  ],
};

test.beforeEach(async ({ page }) => {
  await page.route('**/api/public/discovery', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(payload),
    });
  });
});

test('discovery shows map and needs; text and drawer work', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('discovery-page')).toBeVisible();
  await expect(page.getByTestId('discovery-map')).toBeVisible();
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toBeVisible();

  await page.getByTestId('discovery-search').fill('aleppo');
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toHaveCount(0);

  await page.getByText('Water tanks').click();
  await expect(page.getByTestId('discovery-drawer')).toBeVisible();
  await expect(page.getByTestId('discovery-open-request')).toHaveAttribute(
    'href',
    '/r/camp-a',
  );
});
```

Pin-click filtering is optional in Playwright if Leaflet markers are hard to click reliably; cover pin filter in Vitest already. Prefer attempting marker click only if stable; otherwise document pin filter as unit-tested only.

- [ ] **Step 2: Run Playwright**

```bash
cd needrelay-frontend
yarn playwright test e2e/discovery.spec.ts
```

Expected: PASS.

- [ ] **Step 3: Delete `landing.spec.ts`; commit** (skip if no git)

```bash
git rm needrelay-frontend/e2e/landing.spec.ts
git add needrelay-frontend/e2e/discovery.spec.ts
git commit -m "test: replace landing e2e with discovery coverage"
```

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| `/` replaces landing; header unchanged | 5 |
| Map = ACTIVE pins; list = needs | 1, 5 |
| Pin click + free-text filter | 3, 4, 5 |
| Need drawer + link `/r/:slug` | 5, 6 |
| `GET /api/public/discovery` DTOs | 1, 2 |
| OPEN + PARTIALLY_COVERED only | 1 |
| Empty / error / retry | 5 |
| Backend IT + Playwright | 1, 6 |
| No offers from drawer | out of scope (by design) |

## Self-review notes

- No pagination/server search in plan (matches spec out-of-scope).
- Non-ACTIVE requests use `ReliefRequestStatus.ARCHIVED` in the IT.
- Commit steps skipped when workspace has no git root.
