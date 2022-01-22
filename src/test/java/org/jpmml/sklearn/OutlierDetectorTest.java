/*
 * Copyright (c) 2019 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sklearn;

import org.jpmml.converter.FieldNameUtil;
import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.junit.Test;
import sklearn.Estimator;

public class OutlierDetectorTest extends SkLearnEncoderBatchTest implements SkLearnAlgorithms, SkLearnDatasets {

	@Test
	public void evaluateIsolationForestHousing() throws Exception {
		evaluate(ISOLATION_FOREST, HOUSING, excludeFields("rawAnomalyScore", "normalizedAnomalyScore", OutlierDetectorTest.predictedValue), new PMMLEquivalence(5e-12, 5e-12));
	}

	@Test
	public void evaluateOneClassSVMHousing() throws Exception {
		evaluate(ONE_CLASS_SVM, HOUSING, excludeFields(OutlierDetectorTest.predictedValue));
	}

	private static final String predictedValue = FieldNameUtil.create(Estimator.FIELD_PREDICT, "outlier");
}