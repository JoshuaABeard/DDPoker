# Web Client Feature Parity R6 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add three desktop-parity features to the web client: statistics viewer (profile-embedded), server-stored tournament templates, and showdown analysis (simulator extension).

**Architecture:** Feature 1 (stats) is purely frontend — a new component on the profile page using the existing `/api/history` endpoint. Feature 2 (templates) is full-stack — new JPA entity, Spring controller, and web client UI. Feature 3 (showdown) extends the existing equity calculator and simulator component.

**Tech Stack:** Next.js 16 / React 19 / TypeScript (frontend), Spring Boot 3.5 / Hibernate 6 / H2 (backend), Vitest (frontend tests), Maven (backend tests)

---

### Task 1: Extend `calculateTournamentStats` with profit/loss and best finish

**Files:**
- Modify: `code/web/lib/mappers.ts:47-70`
- Test: `code/web/lib/__tests__/mappers.test.ts` (create)

**Step 1: Write the failing test**

Create `code/web/lib/__tests__/mappers.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { calculateTournamentStats } from '../mappers'
import type { TournamentHistoryDto } from '../types'

const ENTRIES: TournamentHistoryDto[] = [
  { id: 1, name: 'Game 1', placement: 1, buyIn: 100, prize: 500, totalPlayers: 5 },
  { id: 2, name: 'Game 2', placement: 3, buyIn: 100, prize: 50, totalPlayers: 6 },
  { id: 3, name: 'Game 3', placement: 2, buyIn: 200, prize: 300, totalPlayers: 4 },
]

describe('calculateTournamentStats', () => {
  it('computes totalGames, totalWins, totalPrize, avgPlacement, winRate', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.totalGames).toBe(3)
    expect(stats.totalWins).toBe(1)
    expect(stats.totalPrize).toBe(850)
    expect(stats.avgPlacement).toBeCloseTo(2)
    expect(stats.winRate).toBeCloseTo(33.33, 0)
  })

  it('computes totalBuyIn as sum of all buy-ins', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.totalBuyIn).toBe(400)
  })

  it('computes profitLoss as totalPrize minus totalBuyIn', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.profitLoss).toBe(450)
  })

  it('computes bestFinish as lowest placement number', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.bestFinish).toBe(1)
  })

  it('returns zero defaults for empty array', () => {
    const stats = calculateTournamentStats([])
    expect(stats.totalGames).toBe(0)
    expect(stats.totalBuyIn).toBe(0)
    expect(stats.profitLoss).toBe(0)
    expect(stats.bestFinish).toBe(0)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/__tests__/mappers.test.ts`
Expected: FAIL — `totalBuyIn`, `profitLoss`, `bestFinish` properties don't exist on return type.

**Step 3: Write minimal implementation**

Update `calculateTournamentStats` in `code/web/lib/mappers.ts:47-70` to add the three new fields:

```typescript
export function calculateTournamentStats(entries: TournamentHistoryDto[]) {
  if (entries.length === 0) {
    return {
      totalGames: 0,
      totalWins: 0,
      totalPrize: 0,
      totalBuyIn: 0,
      profitLoss: 0,
      bestFinish: 0,
      avgPlacement: 0,
      winRate: 0,
    }
  }

  const totalWins = entries.filter((e) => (e.placement || e.place) === 1).length
  const totalPrize = entries.reduce((sum, e) => sum + (e.prize || 0), 0)
  const totalBuyIn = entries.reduce((sum, e) => sum + (e.buyIn || 0), 0)
  const avgPlacement =
    entries.reduce((sum, e) => sum + (e.placement || e.place || 0), 0) / entries.length
  const bestFinish = Math.min(...entries.map((e) => e.placement || e.place || Infinity))

  return {
    totalGames: entries.length,
    totalWins,
    totalPrize,
    totalBuyIn,
    profitLoss: totalPrize - totalBuyIn,
    bestFinish: bestFinish === Infinity ? 0 : bestFinish,
    avgPlacement,
    winRate: (totalWins / entries.length) * 100,
  }
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/__tests__/mappers.test.ts`
Expected: PASS — all 5 tests green.

**Step 5: Commit**

```bash
git add code/web/lib/mappers.ts code/web/lib/__tests__/mappers.test.ts
git commit -m "feat(web): extend calculateTournamentStats with profit/loss and best finish"
```

---

### Task 2: Create `PlayerStatsSection` component with summary stats and P/L chart

**Files:**
- Create: `code/web/components/profile/PlayerStatsSection.tsx`
- Test: `code/web/components/profile/__tests__/PlayerStatsSection.test.tsx` (create)

**Step 1: Write the failing test**

Create `code/web/components/profile/__tests__/PlayerStatsSection.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { PlayerStatsSection } from '../PlayerStatsSection'

// Mock the tournament API
vi.mock('@/lib/api', () => ({
  tournamentApi: {
    getHistory: vi.fn(),
  },
}))

import { tournamentApi } from '@/lib/api'

const MOCK_HISTORY = {
  history: [
    { id: 1, name: 'Game 1', placement: 1, buyIn: 100, prize: 500, totalPlayers: 5, endDate: '2026-01-15' },
    { id: 2, name: 'Game 2', placement: 3, buyIn: 100, prize: 50, totalPlayers: 6, endDate: '2026-01-20' },
    { id: 3, name: 'Game 3', placement: 2, buyIn: 200, prize: 300, totalPlayers: 4, endDate: '2026-02-01' },
  ],
  total: 3,
}

describe('PlayerStatsSection', () => {
  beforeEach(() => {
    vi.mocked(tournamentApi.getHistory).mockResolvedValue(MOCK_HISTORY)
  })

  it('renders summary stats after loading', async () => {
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument() // total games
    })

    // Check that key stat labels exist
    expect(screen.getByText(/Games Played/i)).toBeInTheDocument()
    expect(screen.getByText(/Win Rate/i)).toBeInTheDocument()
    expect(screen.getByText(/Profit/i)).toBeInTheDocument()
  })

  it('renders the P/L chart SVG', async () => {
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByTestId('pl-chart')).toBeInTheDocument()
    })
  })

  it('shows loading state initially', () => {
    render(<PlayerStatsSection username="TestPlayer" />)
    expect(screen.getByText(/Loading/i)).toBeInTheDocument()
  })

  it('shows empty state when no history', async () => {
    vi.mocked(tournamentApi.getHistory).mockResolvedValue({ history: [], total: 0 })
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByText(/No tournament history/i)).toBeInTheDocument()
    })
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/profile/__tests__/PlayerStatsSection.test.tsx`
Expected: FAIL — module not found.

**Step 3: Write minimal implementation**

Create `code/web/components/profile/PlayerStatsSection.tsx`:

This component should:
1. Fetch all tournament history for the given username (paginate through all pages to get complete data)
2. Compute stats using `calculateTournamentStats`
3. Render a summary stats bar (5 stats in a grid)
4. Render a cumulative P/L SVG line chart
5. Render a sortable history table with pagination

**Key implementation details:**

- Summary stats bar: grid of 5 stat cards showing Total Games, Win Rate, Best Finish, Avg Placement, Profit/Loss
- P/L Chart: SVG with viewBox, polyline for cumulative profit/loss, green/red coloring, x-axis dates, y-axis amounts
- History table: sortable by clicking column headers, show 10 rows per page with prev/next pagination
- Use `tournamentApi.getHistory(username, 0, 1000)` to fetch all history at once for stats (the stats need complete data)
- Format chips/currency with `toLocaleString()`

**Reference patterns:**
- Component style: match `code/web/app/online/myprofile/page.tsx` (bg-white rounded-lg shadow-md p-6)
- API usage: `import { tournamentApi } from '@/lib/api'`
- Stats computation: `import { calculateTournamentStats, mapTournamentEntry } from '@/lib/mappers'`

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/profile/__tests__/PlayerStatsSection.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/components/profile/PlayerStatsSection.tsx code/web/components/profile/__tests__/PlayerStatsSection.test.tsx
git commit -m "feat(web): add PlayerStatsSection with summary stats and P/L chart"
```

---

### Task 3: Integrate `PlayerStatsSection` into profile page

**Files:**
- Modify: `code/web/app/online/myprofile/page.tsx:14,93-94`

**Step 1: Add import and component**

In `code/web/app/online/myprofile/page.tsx`:

1. Add import at line 14:
```typescript
import { PlayerStatsSection } from '@/components/profile/PlayerStatsSection'
```

2. After the `<AliasManagement>` component (line 93), add:
```tsx
<PlayerStatsSection username={user.username} />
```

This places the stats section between Alias Management and Quick Links, as designed.

**Step 2: Verify it renders**

Run: `cd code/web && npx vitest run` (full test suite to check for regressions)
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/app/online/myprofile/page.tsx
git commit -m "feat(web): integrate PlayerStatsSection into profile page"
```

---

### Task 4: Create `TournamentTemplate` JPA entity

**Files:**
- Create: `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentTemplate.java`
- Modify: `code/gameserver/src/main/resources/h2-init.sql:70` (add DDL)
- Modify: `code/pokerserver/src/main/resources/META-INF/persistence-pokerserver.xml:42` (register entity)

**Step 1: Add DDL to h2-init.sql**

Append to end of `code/gameserver/src/main/resources/h2-init.sql`:

```sql
CREATE TABLE IF NOT EXISTS wan_template (
    wtp_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wtp_profile_id INT NOT NULL,
    wtp_name VARCHAR(100) NOT NULL,
    wtp_config TEXT NOT NULL,
    wtp_create_date DATETIME NOT NULL,
    wtp_modify_date DATETIME NOT NULL,
    FOREIGN KEY (wtp_profile_id) REFERENCES wan_profile(wpr_id)
);
CREATE INDEX IF NOT EXISTS wtp_profile_id ON wan_template(wtp_profile_id);
```

**Step 2: Create JPA entity**

Create `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentTemplate.java`:

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * ... (GPL-3.0 header — use Template 3 from copyright guide) ...
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.model;

import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "wan_template")
public class TournamentTemplate implements BaseModel<Long> {

    private Long id;
    private Long profileId;
    private String name;
    private String config;
    private Date createDate;
    private Date modifyDate;

    public TournamentTemplate() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wtp_id", nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "wtp_profile_id", nullable = false)
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    @Column(name = "wtp_name", nullable = false, length = 100)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "wtp_config", nullable = false, columnDefinition = "TEXT")
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    @Column(name = "wtp_create_date", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    @Column(name = "wtp_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModifyDate() { return modifyDate; }
    public void setModifyDate(Date modifyDate) { this.modifyDate = modifyDate; }

    @PrePersist
    private void onInsert() {
        setCreateDate(new Date());
        setModifyDate(new Date());
    }

    @PreUpdate
    private void onUpdate() {
        setModifyDate(new Date());
    }
}
```

**Step 3: Register entity in persistence.xml**

In `code/pokerserver/src/main/resources/META-INF/persistence-pokerserver.xml`, add after line 42:
```xml
    <class>com.donohoedigital.games.poker.model.TournamentTemplate</class>
```

**Step 4: Build to verify compilation**

Run: `cd code && mvn compile -pl pokerengine -q`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentTemplate.java \
        code/gameserver/src/main/resources/h2-init.sql \
        code/pokerserver/src/main/resources/META-INF/persistence-pokerserver.xml
git commit -m "feat(api): add TournamentTemplate JPA entity and DDL"
```

---

### Task 5: Create `TournamentTemplateRepository`

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/TournamentTemplateRepository.java`

**Step 1: Create repository**

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * ... (GPL-3.0 header) ...
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import com.donohoedigital.games.poker.model.TournamentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentTemplateRepository extends JpaRepository<TournamentTemplate, Long> {
    List<TournamentTemplate> findByProfileIdOrderByModifyDateDesc(Long profileId);
}
```

**Step 2: Build to verify**

Run: `cd code && mvn compile -pl pokergameserver -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/TournamentTemplateRepository.java
git commit -m "feat(api): add TournamentTemplateRepository"
```

---

### Task 6: Create `TemplateController` with CRUD endpoints

**Files:**
- Create: `code/api/src/main/java/com/donohoedigital/poker/api/controller/TemplateController.java`
- Create: `code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateRequest.java`
- Create: `code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateResponse.java`

**Step 1: Create DTO classes**

`code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateRequest.java`:
```java
package com.donohoedigital.poker.api.dto;

public class TemplateRequest {
    private String name;
    private String config; // JSON string of game settings

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
```

`code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateResponse.java`:
```java
package com.donohoedigital.poker.api.dto;

import java.util.Date;

public class TemplateResponse {
    private Long id;
    private String name;
    private String config;
    private Date createdDate;
    private Date modifiedDate;

    public TemplateResponse() {}

    public TemplateResponse(Long id, String name, String config, Date createdDate, Date modifiedDate) {
        this.id = id;
        this.name = name;
        this.config = config;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}
```

**Step 2: Create controller**

`code/api/src/main/java/com/donohoedigital/poker/api/controller/TemplateController.java`:

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * ... (GPL-3.0 header) ...
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentTemplateRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentTemplate;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.poker.api.dto.TemplateRequest;
import com.donohoedigital.poker.api.dto.TemplateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile/templates")
public class TemplateController {

    @Autowired
    private TournamentTemplateRepository templateRepository;

    @Autowired
    private OnlineProfileService profileService;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listTemplates(@AuthenticationPrincipal String username) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) return ResponseEntity.notFound().build();

        List<TemplateResponse> templates = templateRepository
                .findByProfileIdOrderByModifyDateDesc(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@AuthenticationPrincipal String username,
            @RequestBody TemplateRequest request) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) return ResponseEntity.notFound().build();

        TournamentTemplate template = new TournamentTemplate();
        template.setProfileId(profile.getId());
        template.setName(request.getName());
        template.setConfig(request.getConfig());
        template = templateRepository.save(template);

        return ResponseEntity.ok(toResponse(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(@AuthenticationPrincipal String username,
            @PathVariable Long id, @RequestBody TemplateRequest request) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) return ResponseEntity.notFound().build();

        return templateRepository.findById(id)
                .filter(t -> t.getProfileId().equals(profile.getId()))
                .map(t -> {
                    t.setName(request.getName());
                    t.setConfig(request.getConfig());
                    return ResponseEntity.ok(toResponse(templateRepository.save(t)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@AuthenticationPrincipal String username,
            @PathVariable Long id) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) return ResponseEntity.notFound().build();

        return templateRepository.findById(id)
                .filter(t -> t.getProfileId().equals(profile.getId()))
                .map(t -> {
                    templateRepository.delete(t);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private TemplateResponse toResponse(TournamentTemplate t) {
        return new TemplateResponse(t.getId(), t.getName(), t.getConfig(), t.getCreateDate(), t.getModifyDate());
    }
}
```

**Step 3: Build and verify**

Run: `cd code && mvn compile -pl api -q`
Expected: BUILD SUCCESS

Note: No security config change needed — `/api/profile/templates` is already covered by `/api/profile/**` which requires authentication (SecurityConfig line 70).

**Step 4: Commit**

```bash
git add code/api/src/main/java/com/donohoedigital/poker/api/controller/TemplateController.java \
        code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateRequest.java \
        code/api/src/main/java/com/donohoedigital/poker/api/dto/TemplateResponse.java
git commit -m "feat(api): add TemplateController with CRUD endpoints for tournament templates"
```

---

### Task 7: Add `templateApi` to web client API layer

**Files:**
- Modify: `code/web/lib/api.ts` (add new API functions after `tournamentApi`)
- Modify: `code/web/lib/types.ts` (add TemplateDto type)

**Step 1: Add type definition**

In `code/web/lib/types.ts`, add after `TournamentHistoryDto` (after line 162):

```typescript
export interface TemplateDto {
  id: number
  name: string
  config: string
  createdDate: string
  modifiedDate: string
}
```

**Step 2: Add API functions**

In `code/web/lib/api.ts`, add import for `TemplateDto` in the import block (line 8-25), then add after `tournamentApi` block (after line 418):

```typescript
/**
 * Tournament Template API
 */
export const templateApi = {
  list: async (): Promise<TemplateDto[]> => {
    const response = await apiFetch<TemplateDto[]>('/api/profile/templates')
    return response.data
  },

  create: async (name: string, config: object): Promise<TemplateDto> => {
    const response = await apiFetch<TemplateDto>('/api/profile/templates', {
      method: 'POST',
      body: JSON.stringify({ name, config: JSON.stringify(config) }),
    })
    return response.data
  },

  update: async (id: number, name: string, config: object): Promise<TemplateDto> => {
    const response = await apiFetch<TemplateDto>(`/api/profile/templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name, config: JSON.stringify(config) }),
    })
    return response.data
  },

  delete: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/profile/templates/${id}`, { method: 'DELETE' })
  },
}
```

**Step 3: Commit**

```bash
git add code/web/lib/api.ts code/web/lib/types.ts
git commit -m "feat(web): add templateApi client for tournament template CRUD"
```

---

### Task 8: Add template load/save UI to create game page

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Add template management**

In `code/web/app/games/create/page.tsx`:

1. Add import: `import { templateApi } from '@/lib/api'` and `import type { TemplateDto } from '@/lib/types'`

2. Add state variables (after line 117):
```typescript
const [templates, setTemplates] = useState<TemplateDto[]>([])
const [selectedTemplateId, setSelectedTemplateId] = useState<string>('')
const [templateName, setTemplateName] = useState('')
const [showSaveDialog, setShowSaveDialog] = useState(false)
const [templateLoading, setTemplateLoading] = useState(false)
```

3. Add `useEffect` to load templates on mount:
```typescript
useEffect(() => {
  templateApi.list().then(setTemplates).catch(() => {})
}, [])
```

4. Add helper functions:
   - `loadTemplate(id)` — finds template by id, parses config JSON, calls all setState functions
   - `saveAsTemplate()` — gathers current form state into config object, calls `templateApi.create()`
   - `deleteTemplate(id)` — calls `templateApi.delete()`, refreshes list

5. Add template toolbar JSX at the top of the form (before "Basic Settings" heading), styled as a compact bar:
   - Select dropdown with templates list (empty option: "-- Select Template --")
   - "Load" button (disabled when no template selected)
   - "Save Current Settings" button (opens name prompt)
   - "Delete" button (with confirmation, disabled when no template selected)
   - Save dialog: text input for template name + Save/Cancel buttons

**Key detail:** When loading a template, parse the config JSON and populate ALL form state:
```typescript
function loadTemplate(id: string) {
  const template = templates.find(t => t.id === Number(id))
  if (!template) return
  const cfg = JSON.parse(template.config)
  setMaxPlayers(cfg.maxPlayers ?? 9)
  setStartingChips(cfg.startingChips ?? 10000)
  setDefaultGameType(cfg.defaultGameType ?? 'NOLIMIT_HOLDEM')
  setBlindStructure(cfg.blindStructure ?? [...DEFAULT_BLIND_STRUCTURE])
  // ... all other fields
}
```

When saving, gather current state:
```typescript
function gatherConfig() {
  return {
    maxPlayers, startingChips, defaultGameType, blindStructure, levelAdvanceMode,
    handsPerLevel, fillComputer, aiPlayerList, payoutSpots, bountyEnabled, bountyAmount,
    allowRebuys, rebuyCost, rebuyChips, rebuyLimit, allowAddon, addonCost, addonChips,
    actionTimeoutSeconds, bootSitout, bootDisconnect, bootAfterHands,
    lateRegistration, lateRegUntilLevel, aiFaceUp, pauseAllin, autoDeal, zipMode,
  }
}
```

**Step 2: Verify it works**

Run: `cd code/web && npx vitest run`
Expected: PASS (existing tests should still pass)

**Step 3: Commit**

```bash
git add code/web/app/games/create/page.tsx
git commit -m "feat(web): add template load/save/delete UI to game creation page"
```

---

### Task 9: Extend equity calculator to accept known opponent hands

**Files:**
- Modify: `code/web/lib/poker/equityCalculator.ts:17-74`
- Modify: `code/web/lib/poker/types.ts:42-47`
- Test: `code/web/lib/poker/__tests__/equityCalculator.test.ts:11-40`

**Step 1: Write the failing tests**

Add to `code/web/lib/poker/__tests__/equityCalculator.test.ts`:

```typescript
describe('known opponent hands', () => {
  it('AA vs known 72o has ~88%+ equity', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 1, 5000, [['7c', '2s']])
    expect(result.win).toBeGreaterThan(85)
  })

  it('returns per-opponent breakdown when opponents specified', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 2, 1000, [['Kh', 'Ks']])
    expect(result.opponentResults).toBeDefined()
    expect(result.opponentResults).toHaveLength(2)
    // First opponent (KK) should have some win percentage
    expect(result.opponentResults![0].win).toBeGreaterThan(0)
  })

  it('mixes known and random opponents', () => {
    // 1 known + 1 random = 2 total opponents
    const result = calculateEquity(['Ah', 'Ad'], [], 2, 1000, [['Kh', 'Ks']])
    expect(result.opponentResults).toHaveLength(2)
    const total = result.win + result.tie + result.loss
    expect(total).toBeGreaterThan(99)
    expect(total).toBeLessThan(101)
  })

  it('works with no known opponents (backward compatible)', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 1, 1000)
    expect(result.opponentResults).toBeUndefined()
    expect(result.win).toBeGreaterThan(75)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/poker/__tests__/equityCalculator.test.ts`
Expected: FAIL — `opponentResults` not in return type, `knownOpponentHands` param not accepted.

**Step 3: Update types**

In `code/web/lib/poker/types.ts`, update `EquityResult` (lines 42-47):

```typescript
export interface EquityResult {
  win: number // percentage 0-100
  tie: number
  loss: number
  iterations: number
  opponentResults?: { win: number; tie: number; loss: number }[]
}
```

**Step 4: Update equity calculator**

Replace `calculateEquity` in `code/web/lib/poker/equityCalculator.ts`:

```typescript
export function calculateEquity(
  holeCards: string[],
  communityCards: string[],
  numOpponents: number,
  iterations: number = 10000,
  knownOpponentHands?: string[][],
): EquityResult {
  const knownOppHands = knownOpponentHands || []
  const randomOpponents = numOpponents - knownOppHands.length

  // Remove all known cards from deck
  const knownCards = [...holeCards, ...communityCards, ...knownOppHands.flat()]
  const remainingDeck = removeCards(FULL_DECK, knownCards)
  const communityNeeded = 5 - communityCards.length

  let wins = 0
  let ties = 0
  let losses = 0
  const oppWins = new Array(numOpponents).fill(0)
  const oppTies = new Array(numOpponents).fill(0)
  const oppLosses = new Array(numOpponents).fill(0)

  for (let i = 0; i < iterations; i++) {
    const shuffled = shuffle([...remainingDeck])
    let dealIndex = 0

    // Deal remaining community cards
    const board = [...communityCards]
    for (let j = 0; j < communityNeeded; j++) {
      board.push(shuffled[dealIndex++])
    }

    // Build opponent hands: known first, then random
    const opponentHands: string[][] = []
    for (const known of knownOppHands) {
      opponentHands.push(known)
    }
    for (let o = 0; o < randomOpponents; o++) {
      opponentHands.push([shuffled[dealIndex++], shuffled[dealIndex++]])
    }

    // Evaluate player hand
    const playerHand = evaluateHand([...holeCards, ...board])

    // Compare against all opponents
    let playerWins = true
    let playerTies = false
    for (let o = 0; o < opponentHands.length; o++) {
      const oppHand = evaluateHand([...opponentHands[o], ...board])
      const cmp = compareHands(playerHand, oppHand)
      if (cmp < 0) {
        playerWins = false
        oppWins[o]++
      } else if (cmp === 0) {
        playerTies = true
        oppTies[o]++
      } else {
        oppLosses[o]++
      }
    }

    if (!playerWins) losses++
    else if (playerTies) ties++
    else wins++
  }

  const result: EquityResult = {
    win: (wins / iterations) * 100,
    tie: (ties / iterations) * 100,
    loss: (losses / iterations) * 100,
    iterations,
  }

  if (knownOpponentHands && knownOpponentHands.length > 0) {
    result.opponentResults = opponentHands_toResults(oppWins, oppTies, oppLosses, iterations, numOpponents)
  }

  return result
}

function opponentHands_toResults(
  wins: number[], ties: number[], losses: number[], iterations: number, count: number
): { win: number; tie: number; loss: number }[] {
  const results = []
  for (let i = 0; i < count; i++) {
    results.push({
      win: (wins[i] / iterations) * 100,
      tie: (ties[i] / iterations) * 100,
      loss: (losses[i] / iterations) * 100,
    })
  }
  return results
}
```

**Step 5: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/poker/__tests__/equityCalculator.test.ts`
Expected: PASS — all tests green (old + new).

**Step 6: Commit**

```bash
git add code/web/lib/poker/equityCalculator.ts code/web/lib/poker/types.ts code/web/lib/poker/__tests__/equityCalculator.test.ts
git commit -m "feat(web): extend equity calculator to accept known opponent hands"
```

---

### Task 10: Add opponent hand specification to Simulator UI

**Files:**
- Modify: `code/web/components/game/Simulator.tsx`

**Step 1: Add opponent hand state and UI**

In `code/web/components/game/Simulator.tsx`:

1. Add new state for opponent hands:
```typescript
const [opponentHands, setOpponentHands] = useState<(string | null)[][]>([])
const [activeOpponentSlot, setActiveOpponentSlot] = useState<{ oppIndex: number; cardIndex: number } | null>(null)
const [showOpponentHands, setShowOpponentHands] = useState(false)
```

2. Update `opponents` state effect — when opponent count changes, resize `opponentHands` array:
```typescript
useEffect(() => {
  setOpponentHands(prev => {
    const next = [...prev]
    while (next.length < opponents) next.push([null, null])
    return next.slice(0, opponents)
  })
}, [opponents])
```

3. Extend `allSelected` to include known opponent cards:
```typescript
const allSelected = [
  ...holeCards.filter((c): c is string => c !== null),
  ...communityCards.filter((c): c is string => c !== null),
  ...opponentHands.flatMap(h => h.filter((c): c is string => c !== null)),
]
```

4. Update `handleCalculate` to pass known opponent hands:
```typescript
function handleCalculate() {
  const hole = holeCards.filter((c): c is string => c !== null)
  if (hole.length < 2) return
  const community = communityCards.filter((c): c is string => c !== null)
  const known = opponentHands
    .filter(h => h[0] !== null && h[1] !== null)
    .map(h => h as string[])

  setIsCalculating(true)
  const result = calculateEquity(hole, community, opponents, 10000, known.length > 0 ? known : undefined)
  setResults(result)
  setIsCalculating(false)
}
```

5. Extend `handleClearAll` to also clear opponent hands.

6. Add UI section after opponent count controls: a "Specify Hands" toggle button. When expanded, show a row per opponent with two card slots each (reuse the `CardSlot` component) and a "Clear" button per opponent. When an opponent slot is clicked, show the `CardPicker` with all already-used cards disabled.

7. Update results section: when `results.opponentResults` exists, show a breakdown per opponent below the main Win/Tie/Loss display.

**Step 2: Verify it works**

Run: `cd code/web && npx vitest run`
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/Simulator.tsx
git commit -m "feat(web): add opponent hand specification to Simulator UI"
```

---

### Task 11: Run full test suite and verify

**Step 1: Run web tests**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 2: Run Java tests**

Run: `cd code && mvn test -P dev -pl api,pokergameserver,pokerengine`
Expected: BUILD SUCCESS, all tests pass

**Step 3: Final commit (if any fixes needed)**

If any tests fail, fix them and commit the fixes.
