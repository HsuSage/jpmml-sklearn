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
package sklearn.isotonic;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.converter.regression.RegressionModelUtil;
import org.jpmml.python.ClassDictUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Regressor;
import sklearn.Transformer;

public class IsotonicRegression extends Regressor {

	public IsotonicRegression(String module, String name){
		super(module, name);
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		PMMLEncoder encoder = schema.getEncoder();

		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		features = encodeFeatures((List)features, (SkLearnEncoder)encoder);

		Feature feature = Iterables.getOnlyElement(features);

		return RegressionModelUtil.createRegression(Collections.singletonList(feature), Collections.singletonList(1d), 0d, RegressionModel.NormalizationMethod.NONE, schema);
	}

	/**
	 * @see Transformer#encodeFeatures(List, SkLearnEncoder)
	 */
	public List<Feature> encodeFeatures(List<Feature> features, SkLearnEncoder encoder){
		List<? extends Number> xThresholds = getXThresholds();
		List<? extends Number> yThresholds = getYThresholds();
		String outOfBounds = getOutOfBounds();

		ClassDictUtil.checkSize(xThresholds, yThresholds);

		SchemaUtil.checkSize(1, features);

		Feature feature = features.get(0);

		OutlierTreatmentMethod outlierTreatment = parseOutlierTreatment(outOfBounds);

		NormContinuous normContinuous = new NormContinuous(feature.getName(), null)
			.setOutliers(outlierTreatment);

		for(int i = 0; i < xThresholds.size(); i++){
			Number orig = xThresholds.get(i);
			Number norm = yThresholds.get(i);

			normContinuous.addLinearNorms(new LinearNorm(orig, norm));
		}

		DerivedField derivedField = encoder.createDerivedField(createFieldName("isotonicRegression", feature), OpType.CONTINUOUS, DataType.DOUBLE, normContinuous);

		return Collections.singletonList(new ContinuousFeature(encoder, derivedField));
	}

	public Boolean getIncreasing(){
		return getBoolean("increasing_");
	}

	public List<? extends Number> getXThresholds(){
		// SkLearn 0.23
		if(containsKey("_necessary_X_")){
			return getNumberArray("_necessary_X_");
		}

		// SkLearn 0.24+
		return getNumberArray("X_thresholds_");
	}

	public List<? extends Number> getYThresholds(){
		// SkLearn 0.23
		if(containsKey("_necessary_y_")){
			getNumberArray("_necessary_y_");
		}

		// SkLearn 0.24+
		return getNumberArray("y_thresholds_");
	}

	public String getOutOfBounds(){
		return getString("out_of_bounds");
	}

	static
	public OutlierTreatmentMethod parseOutlierTreatment(String outOfBounds){

		switch(outOfBounds){
			case "nan":
				return OutlierTreatmentMethod.AS_MISSING_VALUES;
			case "clip":
				return OutlierTreatmentMethod.AS_EXTREME_VALUES;
			default:
				throw new IllegalArgumentException(outOfBounds);
		}
	}
}