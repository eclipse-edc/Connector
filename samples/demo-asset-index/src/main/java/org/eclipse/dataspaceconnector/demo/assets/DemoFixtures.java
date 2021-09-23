/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.assets;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Optional;

final class DemoFixtures {
    static final Fixture[] FIXTURES = {
            Fixture.builder()
                    .id("91cbe938-e4dd-434d-b1df-985407e71a50")
                    .title("IDSA Rule Book")
                    .version("1.0")
                    .year("2020")
                    .category("white paper")
                    .size("4MB")
                    .pages("60")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-Position-Paper-GAIA-X-and-IDS.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("91cbe938-e4dd-434d-b1df-985407e71a50")
                    .title("IDSA Rule Book")
                    .version("1.0")
                    .year("2020")
                    .category("white paper")
                    .size("4MB")
                    .pages("60")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-Position-Paper-GAIA-X-and-IDS.ppt")
                    .type("ppt").build(),

            Fixture.builder()
                    .id("a7514529-9ec4-4770-9661-99a0d3bb5114")
                    .title("GAIA-X and IDS")
                    .version("1.0")
                    .year("2021")
                    .category("position paper")
                    .size("3MB")
                    .pages("33")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-White-Paper-IDSA-Rule-Book.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("a7514529-9ec4-4770-9661-99a0d3bb5114")
                    .title("GAIA-X and IDS")
                    .version("1.0")
                    .year("2021")
                    .category("position paper")
                    .size("3MB")
                    .pages("33")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-White-Paper-IDSA-Rule-Book.ppt")
                    .type("ppt").build(),

            Fixture.builder()
                    .id("2d7862fb-3877-489e-ab33-13936f0bdb73")
                    .title("Implementing the European Strategy on Data. Role of IDS")
                    .version("1.0")
                    .year("2020")
                    .category("position paper")
                    .size("1MB")
                    .pages("7")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-Position-Paper-Implementing-European-Data-Strategy-Role-of-IDS1.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("c1ff3df2-c342-451c-b33a-668d04e456db")
                    .title("IDS Infographic: Data Sharing in a Data Space")
                    .category("info graphic")
                    .size("2MB")
                    .pages("1")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDSA-Infographic-Data-Sharing-in-a-Data-Space.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("afd2bac1-7c26-470b-9b16-c00ee56aa693")
                    .title("Use Case Brochure 2018")
                    .year("2018")
                    .category("brochure")
                    .size("3MB")
                    .pages("23")
                    .url("https://internationaldataspaces.org/wp-content/uploads/dlm_uploads/Use-Case-Brochure_2018.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("24324bff-5791-4952-8c8d-6bf5d51d1a7e")
                    .title("IDS Reference Architecture Model")
                    .version("3.0")
                    .year("2019")
                    .category("brochure")
                    .size("3MB")
                    .pages("118")
                    .url("https://internationaldataspaces.org/wp-content/uploads/IDS-Reference-Architecture-Model-3.0-2019.pdf")
                    .type("pdf").build(),

            Fixture.builder()
                    .id("6146ac53-58d8-4a68-958c-581f3d5992fb")
                    .title("IDS Executive Summary")
                    .year("2018")
                    .category("white paper")
                    .size("1MB")
                    .pages("6")
                    .url("https://internationaldataspaces.org/wp-content/uploads/dlm_uploads/Whitepaper-2018.pdf")
                    .type("pdf").build(),
    };

    static class Fixture {
        private String id;
        private String title;
        private String version;
        private String year;
        private String category;
        private String size;
        private String pages;
        private String url;
        private String type;

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getVersion() {
            return version;
        }

        public String getYear() {
            return year;
        }

        public String getCategory() {
            return category;
        }

        public String getSize() {
            return size;
        }

        public String getPages() {
            return pages;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }

        public static Fixture.Builder builder() {
            return new Fixture.Builder();
        }

        public static final class Builder {
            private String id;
            private String title;
            private String version;
            private String year;
            private String category;
            private String size;
            private String pages;
            private String url;
            private String type;

            private Builder() {
            }

            public Fixture.Builder id(String id) {
                this.id = id;
                return this;
            }

            public Fixture.Builder title(String title) {
                this.title = title;
                return this;
            }

            public Fixture.Builder version(String version) {
                this.version = version;
                return this;
            }

            public Fixture.Builder year(String year) {
                this.year = year;
                return this;
            }

            public Fixture.Builder category(String category) {
                this.category = category;
                return this;
            }

            public Fixture.Builder size(String size) {
                this.size = size;
                return this;
            }

            public Fixture.Builder pages(String pages) {
                this.pages = pages;
                return this;
            }

            public Fixture.Builder url(String url) {
                this.url = url;
                return this;
            }

            public Fixture.Builder type(String type) {
                this.type = type;
                return this;
            }

            public Fixture build() {
                final Fixture fixture = new Fixture();
                fixture.version = this.version;
                fixture.year = this.year;
                fixture.url = this.url;
                fixture.size = this.size;
                fixture.title = this.title;
                fixture.pages = this.pages;
                fixture.category = this.category;
                fixture.id = this.id;
                fixture.type = this.type;
                return fixture;
            }
        }
    }

    static final class AssetFactory {
        public static Asset create(final DemoFixtures.Fixture fixture) {
            final Asset.Builder builder = Asset.Builder.newInstance();

            Optional.ofNullable(fixture.getId())
                    .ifPresent(builder::id);

            Optional.ofNullable(fixture.getTitle())
                    .ifPresent(builder::title);

            Optional.ofNullable(fixture.getVersion())
                    .ifPresent(builder::version);

            Optional.ofNullable(fixture.getCategory())
                    .ifPresent(builder::description);

            Optional.ofNullable(fixture.getType())
                    .ifPresent(builder::fileExtension);

            return builder.build();
        }
    }
}
