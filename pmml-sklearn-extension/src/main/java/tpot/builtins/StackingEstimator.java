/*
 * Copyright (c) 2017 Villu Ruusmann
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
package tpot.builtins;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.ScalarLabel;
import org.jpmml.converter.Schema;
import org.jpmml.converter.TypeUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Classifier;
import sklearn.Estimator;
import sklearn.EstimatorUtil;
import sklearn.HasEstimator;
import sklearn.Transformer;

public class StackingEstimator extends Transformer implements HasEstimator<Estimator> {

	public StackingEstimator(String module, String name){
		super(module, name);
	}

	@Override
	public int getNumberOfFeatures(){
		Estimator estimator = getEstimator();

		return estimator.getNumberOfFeatures();
	}

	@Override
	public List<Feature> encodeFeatures(List<Feature> features, SkLearnEncoder encoder){
		Estimator estimator = getEstimator();

		ScalarLabel scalarLabel;

		MiningFunction miningFunction = estimator.getMiningFunction();
		switch(miningFunction){
			case CLASSIFICATION:
				{
					List<?> categories = EstimatorUtil.getClasses(estimator);

					DataType dataType = TypeUtil.getDataType(categories, DataType.STRING);

					scalarLabel = new CategoricalLabel(dataType, categories);
				}
				break;
			case REGRESSION:
				{
					scalarLabel = new ContinuousLabel(DataType.DOUBLE);
				}
				break;
			default:
				throw new IllegalArgumentException();
		}

		Schema schema = new Schema(encoder, scalarLabel, features);

		Model model = estimator.encode(schema);

		encoder.addTransformer(model);

		String name = createFieldName("stack", features);

		List<Feature> result = new ArrayList<>();

		switch(miningFunction){
			case CLASSIFICATION:
			case REGRESSION:
				{
					Feature feature = encoder.exportPrediction(model, name, scalarLabel);

					result.add(feature);
				}
				break;
			default:
				throw new IllegalArgumentException();
		} // End switch

		switch(miningFunction){
			case CLASSIFICATION:
				{
					Classifier classifier = (Classifier)estimator;

					if(classifier.hasProbabilityDistribution()){
						List<?> categories = EstimatorUtil.getClasses(estimator);

						for(Object category : categories){
							Feature feature = encoder.exportProbability(model, FieldNameUtil.create(Classifier.FIELD_PROBABILITY, name, category), category);

							result.add(feature);
						}
					}
				}
				break;
			case REGRESSION:
				break;
			default:
				throw new IllegalArgumentException();
		}

		result.addAll(features);

		return result;
	}

	@Override
	public Estimator getEstimator(){
		return get("estimator", Estimator.class);
	}
}