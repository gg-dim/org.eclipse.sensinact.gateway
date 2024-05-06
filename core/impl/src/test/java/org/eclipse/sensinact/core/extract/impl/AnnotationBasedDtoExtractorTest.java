/*********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.core.extract.impl;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.sensinact.core.annotation.dto.Data;
import org.eclipse.sensinact.core.annotation.dto.Metadata;
import org.eclipse.sensinact.core.annotation.dto.Model;
import org.eclipse.sensinact.core.annotation.dto.ModelPackageUri;
import org.eclipse.sensinact.core.annotation.dto.NullAction;
import org.eclipse.sensinact.core.annotation.dto.Provider;
import org.eclipse.sensinact.core.annotation.dto.Resource;
import org.eclipse.sensinact.core.annotation.dto.Service;
import org.eclipse.sensinact.core.annotation.dto.Timestamp;
import org.eclipse.sensinact.core.dto.impl.AbstractUpdateDto;
import org.eclipse.sensinact.core.dto.impl.DataUpdateDto;
import org.eclipse.sensinact.core.dto.impl.FailedMappingDto;
import org.eclipse.sensinact.core.dto.impl.MetadataUpdateDto;
import org.eclipse.sensinact.model.core.testdata.TestdataPackage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AnnotationBasedDtoExtractorTest {

    private static final String MODEL_PACKAGE_URI = "http://test.org/foo/bar";
    private static final String MODEL = "model";
    private static final String PROVIDER = "provider";
    private static final String SERVICE = "service";
    private static final String RESOURCE = "resource";
    private static final String RESOURCE_2 = "resource2";

    private static final Integer VALUE = 5;
    private static final String VALUE_2 = "Fourteen";

    private static final String METADATA_KEY = "foo";
    private static final String METADATA_VALUE = "fizz";
    private static final String METADATA_VALUE_2 = "buzz";

    @Provider
    @Service(SERVICE)
    @Resource(RESOURCE)
    public static class BasicDtoClassLevelError {
        @Data
        public Integer foo;
    }

    @Provider(PROVIDER)
    @Service
    @Resource(RESOURCE)
    public static class BasicDtoClassLevelError2 {
        @Data
        public Integer foo;
    }

    @ModelPackageUri(MODEL_PACKAGE_URI)
    @Model(MODEL)
    @Provider(PROVIDER)
    @Service(SERVICE)
    @Resource(RESOURCE)
    public static class BasicDtoClassLevel {
        @Data
        public Integer foo;

        @Metadata
        public String units;

        @Metadata(value = METADATA_KEY, onNull = NullAction.UPDATE)
        public String fizzbuzz;
    }

    public static abstract class ProviderAndServiceFields {
        @Provider(PROVIDER)
        public String provider;

        @Service(SERVICE)
        public String service;

        @Timestamp
        public Long time;
    }

    public static class BasicDtoFieldAnnotated extends ProviderAndServiceFields {
        @Resource(RESOURCE)
        @Data
        public Integer foo;

        @Resource(RESOURCE_2)
        @Data
        public String bar;

        @Resource(RESOURCE)
        @Metadata
        public String units;

        @Resource(RESOURCE_2)
        @Metadata(METADATA_KEY)
        public String fizzbuzz;
    }

    public static class BasicDtoFieldNames extends ProviderAndServiceFields {
        @Data
        public Integer foo;

        @Data
        public String bar;
    }

    /**
     * Tests for class level annotations for provider/service/resource
     */
    @Nested
    public class ClassLevelAnnotations {

        @Test
        void missingProviderName() {

            DataExtractor extractor = extractor(BasicDtoClassLevelError.class);
            List<? extends AbstractUpdateDto> updates = extractor.getUpdates(new BasicDtoClassLevel());

            assertNotNull(updates);
            assertEquals(1, updates.size());
            AbstractUpdateDto aud = updates.get(0);
            assertNotNull(aud);
            assertInstanceOf(FailedMappingDto.class, aud);
            FailedMappingDto fmd = (FailedMappingDto) aud;
            assertNotNull(fmd.mappingFailure);

            assertTrue(fmd.mappingFailure.getMessage().contains("not properly defined"),
                    "Wrong message: " + fmd.mappingFailure.getMessage());
            assertTrue(
                    fmd.mappingFailure.getCause().getMessage()
                            .contains("annotated with Provider but that annotation has no value"),
                    "Wrong message: " + fmd.mappingFailure.getCause().getMessage());
        }

        @Test
        void missingServiceName() {

            DataExtractor extractor = extractor(BasicDtoClassLevelError2.class);
            List<? extends AbstractUpdateDto> updates = extractor.getUpdates(new BasicDtoClassLevelError2());

            assertNotNull(updates);
            assertEquals(1, updates.size());
            AbstractUpdateDto aud = updates.get(0);
            assertNotNull(aud);
            assertInstanceOf(FailedMappingDto.class, aud);
            FailedMappingDto fmd = (FailedMappingDto) aud;
            assertNotNull(fmd.mappingFailure);

            assertTrue(fmd.mappingFailure.getMessage().contains("not properly defined"),
                    "Wrong message: " + fmd.mappingFailure.getMessage());
            assertTrue(
                    fmd.mappingFailure.getCause().getMessage()
                            .contains("annotated with Service but that annotation has no value"),
                    "Wrong message: " + fmd.mappingFailure.getCause().getMessage());
        }

        @Test
        void basicDtoNullMetadata() {
            BasicDtoClassLevel dto = new BasicDtoClassLevel();

            dto.foo = VALUE;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoClassLevel.class).getUpdates(dto);

            assertEquals(2, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance).findFirst().get();

            checkCommonFields(extracted);

            assertEquals(MODEL_PACKAGE_URI, extracted.modelPackageUri);
            assertEquals(MODEL, extracted.model);
            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);

            extracted = updates.stream().filter(MetadataUpdateDto.class::isInstance).findFirst().get();

            checkCommonFields(extracted);
            assertTrue(extracted instanceof MetadataUpdateDto, "Not a metadata update dto " + extracted.getClass());

            MetadataUpdateDto dud2 = (MetadataUpdateDto) extracted;

            assertEquals(singletonMap(METADATA_KEY, null), dud2.metadata);
            assertFalse(dud2.removeNullValues, "Null values should be removed");
            assertFalse(dud2.removeMissingValues, "Missing values should be kept");
        }

        @Test
        void basicDtoWithMetadataValues() {
            BasicDtoClassLevel dto = new BasicDtoClassLevel();

            dto.foo = VALUE;
            dto.fizzbuzz = METADATA_VALUE;
            dto.units = METADATA_VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoClassLevel.class).getUpdates(dto);

            assertEquals(3, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance).findFirst().get();

            checkCommonFields(extracted);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);

            extracted = updates.stream().filter(MetadataUpdateDto.class::isInstance)
                    .filter(d -> ((MetadataUpdateDto) d).metadata.containsKey(METADATA_KEY)).findFirst().get();

            checkCommonFields(extracted);
            assertTrue(extracted instanceof MetadataUpdateDto, "Not a metadata update dto " + extracted.getClass());

            MetadataUpdateDto dud2 = (MetadataUpdateDto) extracted;

            assertEquals(singletonMap(METADATA_KEY, METADATA_VALUE), dud2.metadata);
            assertFalse(dud2.removeNullValues, "Null values should be removed");
            assertFalse(dud2.removeMissingValues, "Missing values should be kept");

            extracted = updates.stream().filter(MetadataUpdateDto.class::isInstance)
                    .filter(d -> ((MetadataUpdateDto) d).metadata.containsKey("units")).findFirst().get();

            checkCommonFields(extracted);
            assertTrue(extracted instanceof MetadataUpdateDto, "Not a metadata update dto " + extracted.getClass());

            dud2 = (MetadataUpdateDto) extracted;

            assertEquals(singletonMap("units", METADATA_VALUE_2), dud2.metadata);
            assertFalse(dud2.removeNullValues, "Null values should be removed");
            assertFalse(dud2.removeMissingValues, "Missing values should be kept");
        }
    }

    /**
     * Tests for class level annotations for provider/service/resource
     */
    @Nested
    public class FieldLevelAnnotations {

        @Test
        void basicDtoOneValueNullMetadata() {
            Instant time = Instant.now().minus(Duration.ofDays(3)).truncatedTo(ChronoUnit.MILLIS);

            BasicDtoFieldAnnotated dto = new BasicDtoFieldAnnotated();

            dto.provider = PROVIDER;
            dto.service = SERVICE;
            dto.time = time.toEpochMilli();
            dto.foo = VALUE;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoFieldAnnotated.class).getUpdates(dto);

            assertEquals(1, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance).findFirst().get();

            checkCommonFields(extracted, time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);
        }

        @Test
        void basicDtoWithBothValuesAndMetadataValues() {
            Instant time = Instant.now().minus(Duration.ofDays(3)).truncatedTo(ChronoUnit.MILLIS);

            BasicDtoFieldAnnotated dto = new BasicDtoFieldAnnotated();

            dto.provider = PROVIDER;
            dto.service = SERVICE;
            dto.time = time.toEpochMilli();
            dto.foo = VALUE;
            dto.bar = VALUE_2;
            dto.fizzbuzz = METADATA_VALUE;
            dto.units = METADATA_VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoFieldAnnotated.class).getUpdates(dto);

            assertEquals(4, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance)
                    .filter(d -> RESOURCE.equals(d.resource)).findFirst().get();

            checkCommonFields(extracted, time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);

            extracted = updates.stream().filter(DataUpdateDto.class::isInstance)
                    .filter(d -> RESOURCE_2.equals(d.resource)).findFirst().get();

            checkCommonFields(extracted, false, time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            dud = (DataUpdateDto) extracted;

            assertEquals(VALUE_2, dud.data);
            assertEquals(String.class, dud.type);

            extracted = updates.stream().filter(MetadataUpdateDto.class::isInstance)
                    .filter(d -> ((MetadataUpdateDto) d).metadata.containsKey(METADATA_KEY)).findFirst().get();

            checkCommonFields(extracted, false, time);
            assertTrue(extracted instanceof MetadataUpdateDto, "Not a metadata update dto " + extracted.getClass());

            MetadataUpdateDto dud2 = (MetadataUpdateDto) extracted;

            assertEquals(singletonMap(METADATA_KEY, METADATA_VALUE), dud2.metadata);
            assertFalse(dud2.removeNullValues, "Null values should be removed");
            assertFalse(dud2.removeMissingValues, "Missing values should be kept");

            extracted = updates.stream().filter(MetadataUpdateDto.class::isInstance)
                    .filter(d -> ((MetadataUpdateDto) d).metadata.containsKey("units")).findFirst().get();

            checkCommonFields(extracted, time);
            assertTrue(extracted instanceof MetadataUpdateDto, "Not a metadata update dto " + extracted.getClass());

            dud2 = (MetadataUpdateDto) extracted;

            assertEquals(singletonMap("units", METADATA_VALUE_2), dud2.metadata);
            assertFalse(dud2.removeNullValues, "Null values should be removed");
            assertFalse(dud2.removeMissingValues, "Missing values should be kept");
        }
    }

    /**
     * Tests for resource name defaulting based on field name
     */
    @Nested
    public class FieldNameDefaulting {

        @Test
        void basicDtoOneValueNullMetadata() {
            Instant time = Instant.now().minus(Duration.ofDays(3)).truncatedTo(ChronoUnit.MILLIS);

            BasicDtoFieldNames dto = new BasicDtoFieldNames();

            dto.provider = PROVIDER;
            dto.service = SERVICE;
            dto.time = time.toEpochMilli();
            dto.foo = VALUE;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoFieldNames.class).getUpdates(dto);

            assertEquals(1, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance).findFirst().get();

            checkCommonFields(extracted, "foo", time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);
        }

        @Test
        void basicDtoWithBothValuesAndMetadataValues() {
            Instant time = Instant.now().minus(Duration.ofDays(3)).truncatedTo(ChronoUnit.MILLIS);

            BasicDtoFieldNames dto = new BasicDtoFieldNames();

            dto.provider = PROVIDER;
            dto.service = SERVICE;
            dto.time = time.toEpochMilli();
            dto.foo = VALUE;
            dto.bar = VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(BasicDtoFieldNames.class).getUpdates(dto);

            assertEquals(2, updates.size());

            AbstractUpdateDto extracted = updates.stream().filter(DataUpdateDto.class::isInstance)
                    .filter(d -> "foo".equals(d.resource)).findFirst().get();

            checkCommonFields(extracted, "foo", time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            DataUpdateDto dud = (DataUpdateDto) extracted;

            assertEquals(VALUE, dud.data);
            assertEquals(Integer.class, dud.type);

            extracted = updates.stream().filter(DataUpdateDto.class::isInstance).filter(d -> "bar".equals(d.resource))
                    .findFirst().get();

            checkCommonFields(extracted, "bar", time);

            assertTrue(extracted instanceof DataUpdateDto, "Not a data update dto " + extracted.getClass());

            dud = (DataUpdateDto) extracted;

            assertEquals(VALUE_2, dud.data);
            assertEquals(String.class, dud.type);
        }
    }

    /**
     * Tests for EMF based DTOs
     */
    @Nested
    public class EMFAnnotated {
        @Provider(PROVIDER)
        public class EMFTestDto {

            @Model
            public EClass providerEClass = TestdataPackage.Literals.TEST_SENSOR;

            @Service
            public EReference service = TestdataPackage.Literals.TEST_SENSOR__TEMP;

            @Service
            public EClass serviceEClass = TestdataPackage.Literals.TEST_TEMPERATUR;

            @Data
            public String v1;

        }

        @Provider(PROVIDER)
        public class EMFTestDynamicDto {

            @Model
            public EClass providerEClass = TestdataPackage.Literals.DYNAMIC_TEST_SENSOR;

            @Service
            public EClass serviceEClass = TestdataPackage.Literals.TEST_TEMPERATUR;

            @Service
            public String serviceName = "humidity";

            @Data
            public String v1;

        }

        @Provider(PROVIDER)
        public class EMFTestDynamicFailDto {

            @Model
            public EClass providerEClass = TestdataPackage.Literals.DYNAMIC_TEST_SENSOR;

            @Service
            public EClass serviceEClass = TestdataPackage.Literals.TEST_TEMPERATUR;

            @Data
            public String v1;

        }

        @Provider(PROVIDER)
        public class EMFTestDifferentNameDto {

            @Model
            public EClass providerEClass = TestdataPackage.Literals.TEST_SENSOR;

            @Service
            public EReference service = TestdataPackage.Literals.TEST_SENSOR__TEMP;

            @Service
            public EClass serviceEClass = TestdataPackage.Literals.TEST_TEMPERATUR;

            @Data
            @Service("not_temp")
            public String v1;

        }

        @Test
        void basicDtoWithServiceReference() {
            EMFTestDto dto = new EMFTestDto();
            dto.v1 = VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(EMFTestDto.class).getUpdates(dto);

            assertEquals(1, updates.size());

            DataUpdateDto extracted = updates.stream().findFirst().map(DataUpdateDto.class::cast).get();

            assertEquals(TestdataPackage.Literals.TEST_SENSOR, extracted.modelEClass);
            assertEquals(TestdataPackage.Literals.TEST_SENSOR__TEMP, extracted.serviceReference);
        }

        @Test
        void dtoWithDynamicProviderAndServiceEClass() {
            EMFTestDynamicDto dto = new EMFTestDynamicDto();
            dto.v1 = VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(EMFTestDynamicDto.class).getUpdates(dto);

            assertEquals(1, updates.size());

            DataUpdateDto extracted = updates.stream().findFirst().map(DataUpdateDto.class::cast).get();

            assertEquals(TestdataPackage.Literals.DYNAMIC_TEST_SENSOR, extracted.modelEClass);
            assertEquals(TestdataPackage.Literals.TEST_TEMPERATUR, extracted.serviceEClass);
            assertEquals("humidity", extracted.service);
            assertEquals("v1", extracted.resource);
        }

        @Test
        void differentServiceNames() {
            EMFTestDifferentNameDto dto = new EMFTestDifferentNameDto();
            dto.v1 = VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(EMFTestDifferentNameDto.class).getUpdates(dto);

            assertEquals(1, updates.size());

            FailedMappingDto extracted = updates.stream().findFirst().map(FailedMappingDto.class::cast).get();

            assertNotNull(extracted.mappingFailure);
            assertEquals(
                    "The defined service name not_temp does not match the defined EReference temp for the field v1 in "
                            + EMFTestDifferentNameDto.class,
                    extracted.mappingFailure.getMessage());
        }

        @Test
        void dynamicProviderWithMissingService() {
            EMFTestDynamicFailDto dto = new EMFTestDynamicFailDto();
            dto.v1 = VALUE_2;

            List<? extends AbstractUpdateDto> updates = extractor(EMFTestDynamicFailDto.class).getUpdates(dto);

            assertEquals(1, updates.size());

            FailedMappingDto extracted = updates.stream().findFirst().map(FailedMappingDto.class::cast).get();

            assertNotNull(extracted.mappingFailure);
            assertEquals(
                    "No service or service EReference is defined for the field v1 in " + EMFTestDynamicFailDto.class,
                    extracted.mappingFailure.getMessage());
        }
    }

    DataExtractor extractor(Class<?> clazz) {
        return new CustomDtoDataExtractor(clazz);
    }

    private void checkCommonFields(AbstractUpdateDto extracted) {
        checkCommonFields(extracted, true);
    }

    private void checkCommonFields(AbstractUpdateDto extracted, Instant time) {
        checkCommonFields(extracted, true, time);
    }

    private void checkCommonFields(AbstractUpdateDto extracted, String altResource, Instant time) {
        checkCommonFields(extracted, true, altResource, time);
    }

    private void checkCommonFields(AbstractUpdateDto extracted, boolean use1) {
        checkCommonFields(extracted, use1, null);
    }

    private void checkCommonFields(AbstractUpdateDto extracted, boolean use1, Instant time) {
        checkCommonFields(extracted, use1, null, time);
    }

    private void checkCommonFields(AbstractUpdateDto extracted, boolean use1, String altResource, Instant time) {
        assertEquals(PROVIDER, extracted.provider);
        assertEquals(SERVICE, extracted.service);
        assertEquals(altResource == null ? use1 ? RESOURCE : RESOURCE_2 : altResource, extracted.resource);
        if (time == null) {
            assertTrue(Duration.between(extracted.timestamp, Instant.now()).minusMillis(100).isNegative(),
                    () -> "The timestamp was not set properly got: " + extracted.timestamp + " now is: "
                            + Instant.now());
        } else {
            assertEquals(time, extracted.timestamp);
        }
    }
}
