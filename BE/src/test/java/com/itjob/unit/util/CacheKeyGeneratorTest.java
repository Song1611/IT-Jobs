package com.itjob.unit.util;

import com.itjob.util.CacheKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - CacheKeyGenerator")
class CacheKeyGeneratorTest {

    @Nested
    @DisplayName("forId")
    class ForId {
        @Test
        @DisplayName("returns the id as string")
        void returnsStringId() {
            UUID id = UUID.randomUUID();
            assertThat(CacheKeyGenerator.forId(id)).isEqualTo(id.toString());
        }
    }

    @Nested
    @DisplayName("forSlug")
    class ForSlug {
        @Test
        @DisplayName("returns the slug unchanged")
        void returnsRawSlug() {
            assertThat(CacheKeyGenerator.forSlug("java-developer")).isEqualTo("java-developer");
        }
    }

    @Nested
    @DisplayName("forLimit")
    class ForLimit {
        @Test
        @DisplayName("builds key with limit prefix and value")
        void buildsLimitKey() {
            assertThat(CacheKeyGenerator.forLimit(10)).isEqualTo("limit:10");
        }
    }

    @Nested
    @DisplayName("forPageable")
    class ForPageable {
        @Test
        @DisplayName("without sort -> only page and size")
        void withoutSort() {
            Pageable pageable = PageRequest.of(2, 15);
            assertThat(CacheKeyGenerator.forPageable(pageable))
                    .isEqualTo("page:2:size:15");
        }

        @Test
        @DisplayName("with sort -> appends sort segment")
        void withSort() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            assertThat(CacheKeyGenerator.forPageable(pageable))
                    .isEqualTo("page:0:size:10:sort:createdAt,DESC");
        }
    }

    @Nested
    @DisplayName("forIdWithPageable")
    class ForIdWithPageable {
        @Test
        @DisplayName("builds key with id prefix and pagination info")
        void buildsKey() {
            UUID id = UUID.randomUUID();
            Pageable pageable = PageRequest.of(1, 20);
            assertThat(CacheKeyGenerator.forIdWithPageable(id, pageable))
                    .isEqualTo("id:" + id + ":page:1:size:20");
        }
    }

    @Nested
    @DisplayName("forCompanyPage / forUserPage")
    class ForScopedPages {
        @Test
        @DisplayName("company-scoped key uses double delimiter")
        void companyPage() {
            UUID id = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            assertThat(CacheKeyGenerator.forCompanyPage(id, pageable))
                    .isEqualTo("company:" + id + "::page:0:size:10");
        }

        @Test
        @DisplayName("user-scoped key uses double delimiter")
        void userPage() {
            UUID id = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            assertThat(CacheKeyGenerator.forUserPage(id, pageable))
                    .isEqualTo("user:" + id + "::page:0:size:10");
        }
    }

    @Nested
    @DisplayName("forFilters")
    class ForFilters {
        @Test
        @DisplayName("null -> none")
        void nullFilters() {
            assertThat(CacheKeyGenerator.forFilters(null)).isEqualTo("none");
        }

        @Test
        @DisplayName("empty -> none")
        void emptyFilters() {
            assertThat(CacheKeyGenerator.forFilters(new String[0])).isEqualTo("none");
        }

        @Test
        @DisplayName("sorted regardless of input order")
        void sortedRegardlessOfOrder() {
            String[] input = {"status:open", "level:senior"};
            String[] reversed = {"level:senior", "status:open"};
            assertThat(CacheKeyGenerator.forFilters(input))
                    .isEqualTo(CacheKeyGenerator.forFilters(reversed));
            assertThat(CacheKeyGenerator.forFilters(input)).isEqualTo("[level:senior, status:open]");
        }
    }

    @Nested
    @DisplayName("forSearch")
    class ForSearch {
        @Test
        @DisplayName("sorts params alphabetically and appends pageable")
        void sortsParamsAndAppendsPageable() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("q", "java");
            params.put("loc", "hcm");
            Pageable pageable = PageRequest.of(0, 10);
            assertThat(CacheKeyGenerator.forSearch(params, pageable))
                    .isEqualTo("loc:hcm:q:java:page:0:size:10");
        }

        @Test
        @DisplayName("skips null values")
        void ignoresNullValues() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("q", null);
            params.put("type", "fulltime");
            Pageable pageable = PageRequest.of(0, 10);
            assertThat(CacheKeyGenerator.forSearch(params, pageable))
                    .isEqualTo("type:fulltime:page:0:size:10");
        }
    }

    @Nested
    @DisplayName("forComposite")
    class ForComposite {
        @Test
        @DisplayName("joins sorted key-value pairs as a full deterministic key")
        void sortsAndJoins() {
            UUID companyId = UUID.randomUUID();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("companyId", companyId);
            params.put("status", "open");

            assertThat(CacheKeyGenerator.forComposite(params))
                    .isEqualTo("companyId:" + companyId + ":status:open");
        }
    }

    @Nested
    @DisplayName("Dashboard keys")
    class ForDashboard {
        @Test
        @DisplayName("admin dashboard key constant is stats")
        void admin() {
            assertThat(CacheKeyGenerator.STATS_SUFFIX).isEqualTo("stats");
            assertThat(CacheKeyGenerator.forHRDashboard("x")).endsWith(":stats");
        }

        @Test
        @DisplayName("hr dashboard key contains hr id and stats")
        void hr() {
            UUID id = UUID.randomUUID();
            assertThat(CacheKeyGenerator.forHRDashboard(id)).isEqualTo("hr:" + id + ":stats");
        }

        @Test
        @DisplayName("user dashboard key contains user id and stats")
        void user() {
            UUID id = UUID.randomUUID();
            assertThat(CacheKeyGenerator.forUserDashboard(id)).isEqualTo("user:" + id + ":stats");
        }
    }
}