/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.build;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.build.MainOutputFile;
import com.android.build.OutputFile;
import com.android.build.VariantOutput;
import com.android.resources.Density;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SplitOutputMatcherTest extends TestCase {

    /**
     * Helper to run InstallHelper.computeMatchingOutput with variable ABI list.
     */
    private static List<OutputFile> computeBestOutput(
            @NonNull List<? extends VariantOutput> outputs,
            int density,
            @NonNull String... abis) {
        return SplitOutputMatcher.computeBestOutput(
                outputs, null, density, Arrays.asList(abis));
    }

    private static List<OutputFile> computeBestOutput(
            @NonNull List<? extends VariantOutput> outputs,
            @NonNull Set<String> variantAbis,
            int density,
            @NonNull String... abis) {
        return SplitOutputMatcher.computeBestOutput(
                outputs, variantAbis, density, Arrays.asList(abis));
    }

    /**
     * Fake implementation of FilteredOutput
     */

    private static final class FakeSplitOutput implements MainOutputFile {

        private final String densityFilter;
        private final String abiFilter;
        private final File file;

        FakeSplitOutput(String densityFilter, String abiFilter) {
            this.densityFilter = densityFilter;
            this.abiFilter = abiFilter;
            file = new File(densityFilter + abiFilter);
        }

        @Override
        public String getOutputType() {
            return OutputFile.FULL_SPLIT;
        }

        @NonNull
        @Override
        public Collection<String> getFilterTypes() {
            ImmutableList.Builder<String> splitTypeBuilder = ImmutableList.builder();
            if (densityFilter != null) {
                splitTypeBuilder.add(OutputFile.DENSITY);
            }
            if (abiFilter != null) {
                splitTypeBuilder.add(OutputFile.ABI);
            }
            return splitTypeBuilder.build();
        }

        @NonNull
        @Override
        public Collection<FilterData> getFilters() {
            ImmutableList.Builder<FilterData> filters = ImmutableList.builder();
            if (densityFilter != null) {
                filters.add(FilterData.Builder.build(OutputFile.DENSITY, densityFilter));
            }
            if (abiFilter != null) {
                filters.add(FilterData.Builder.build(OutputFile.ABI, abiFilter));
            }
            return filters.build();
        }

        @NonNull
        @Override
        public File getOutputFile() {
            return file;
        }

        @Override
        public String toString() {
            return "FilteredOutput{" + densityFilter + ':' + abiFilter + '}';
        }
    }

    private static class FakeVariantOutput implements VariantOutput {

        private final MainOutputFile mainOutputFile;
        private final int versionCode;

        private FakeVariantOutput(MainOutputFile mainOutputFile,
                int versionCode) {
            this.mainOutputFile = mainOutputFile;
            this.versionCode = versionCode;
        }

        @NonNull
        @Override
        public MainOutputFile getMainOutputFile() {
            return mainOutputFile;
        }

        @NonNull
        @Override
        public Collection<? extends OutputFile> getOutputs() {
            return ImmutableList.of(mainOutputFile);
        }

        @Override
        public int getVersionCode() {
            return versionCode;
        }

        @NonNull
        @Override
        public File getSplitFolder() {
            return null;
        }
    }

    public void testSingleOutput() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));

        List<OutputFile> result = computeBestOutput(list, 160, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testDensityOnlyWithMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(match = getDensityOutput(160, 2));
        list.add(getDensityOutput(320, 3));

        List<OutputFile> result =  computeBestOutput(list, 160, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testDensityOnlyWithUniversalMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(3));
        list.add(getDensityOutput(320, 2));
        list.add(getDensityOutput(480, 1));

        List<OutputFile> result = computeBestOutput(list, 160, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testDensityOnlyWithNoMatch() {
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getDensityOutput(320, 1));
        list.add(getDensityOutput(480, 2));

        List<OutputFile> result = computeBestOutput(list, 160, "foo");

        assertEquals(0, result.size());
    }

    public void testDensityOnlyWithCustomDeviceDensity() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        list.add(getDensityOutput(320, 2));
        list.add(getDensityOutput(480, 3));

        List<OutputFile> result = computeBestOutput(list, 1, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }


    public void testAbiOnlyWithMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(match = getAbiOutput("foo", 2));
        list.add(getAbiOutput("bar", 3));

        List<OutputFile> result = computeBestOutput(list, 160, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testAbiOnlyWithMultiMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        // test where the versionCode match the abi order
        list.add(getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(match = getAbiOutput("bar", 3));

        // bar is preferred over foo
        List<OutputFile> result = computeBestOutput(list, 160, "bar", "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testAbiOnlyWithMultiMatch2() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        // test where the versionCode does not match the abi order
        list.add(getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(match = getAbiOutput("bar", 3));

        // bar is preferred over foo
        List<OutputFile> result = computeBestOutput(list, 160, "foo", "bar");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testAbiOnlyWithUniversalMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(getAbiOutput("bar", 3));

        List<OutputFile> result = computeBestOutput(list, 160, "zzz");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testAbiOnlyWithNoMatch() {
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getAbiOutput("foo", 1));
        list.add(getAbiOutput("bar", 2));

        List<OutputFile> result = computeBestOutput(list, 160, "zzz");

        assertEquals(0, result.size());
    }

    public void testMultiFilterWithMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(getOutput(160, "zzz",2));
        list.add(match = getOutput(160, "foo", 4));
        list.add(getOutput(320, "foo", 3));

        List<OutputFile> result = computeBestOutput(list, 160, "foo");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testMultiFilterWithUniversalMatch() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(4));
        list.add(getOutput(320, "zzz", 3));
        list.add(getOutput(160, "bar", 2));
        list.add(getOutput(320, "foo", 1));

        List<OutputFile> result = computeBestOutput(list, 160, "zzz");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testMultiFilterWithNoMatch() {
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getOutput(320, "zzz", 1));
        list.add(getOutput(160, "bar", 2));
        list.add(getOutput(320, "foo", 3));

        List<OutputFile> result = computeBestOutput(list, 160, "zzz");

        assertEquals(0, result.size());
    }

    public void testVariantLevelAbiFilter() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        List<OutputFile> result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 160, "foo",
                "zzz");

        assertEquals(1, result.size());
        assertEquals(match.getMainOutputFile(), result.get(0));
    }

    public void testWrongVariantLevelAbiFilter() {
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));

        List<OutputFile> result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 160, "zzz");

        assertEquals(0, result.size());
    }

    public void testDensitySplitPlugVariantLevelAbiFilter() {
        VariantOutput match;
        List<VariantOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(getDensityOutput(240, 2));
        list.add(match = getDensityOutput(320, 3));
        list.add(getDensityOutput(480, 4));

        List<OutputFile> result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 320, "foo", "zzz");

        assertEquals(1, result.size());
    }



    private static VariantOutput getUniversalOutput(int versionCode) {
        return new FakeVariantOutput(new FakeSplitOutput(null, null), versionCode);
    }

    private static VariantOutput getDensityOutput(int densityFilter, int versionCode) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new FakeVariantOutput(
                new FakeSplitOutput(densityEnum.getResourceValue(), null), versionCode);
    }

    private static VariantOutput getAbiOutput(String filter, int versionCode) {
        return new FakeVariantOutput(
                new FakeSplitOutput( null, filter), versionCode);
    }

    private static VariantOutput getOutput(int densityFilter, String abiFilter, int versionCode) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new FakeVariantOutput(
                new FakeSplitOutput(densityEnum.getResourceValue(), abiFilter), versionCode);
    }
}
