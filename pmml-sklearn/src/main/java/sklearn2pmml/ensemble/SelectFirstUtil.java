/*
 * Copyright (c) 2020 Villu Ruusmann
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
package sklearn2pmml.ensemble;

import java.util.List;

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.python.DataFrameScope;
import org.jpmml.python.Scope;
import org.jpmml.python.TupleUtil;
import sklearn.Estimator;
import sklearn2pmml.util.EvaluatableUtil;

public class SelectFirstUtil {

	private SelectFirstUtil(){
	}

	static
	public MiningModel encodeClassifier(List<Object[]> steps, Schema schema){
		return encodeModel(MiningFunction.CLASSIFICATION, steps, schema);
	}

	static
	public MiningModel encodeRegressor(List<Object[]> steps, Schema schema){
		return encodeModel(MiningFunction.REGRESSION, steps, schema);
	}

	static
	private MiningModel encodeModel(MiningFunction miningFunction, List<Object[]> steps, Schema schema){

		if(steps.isEmpty()){
			throw new IllegalArgumentException();
		}

		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		Segmentation segmentation = new Segmentation(Segmentation.MultipleModelMethod.SELECT_FIRST, null);

		Scope scope = new DataFrameScope("X", features);

		for(int i = 0; i < steps.size(); i++){
			Object[] step = steps.get(i);

			String name = TupleUtil.extractElement(step, 0, String.class);
			Estimator estimator = TupleUtil.extractElement(step, 1, Estimator.class);
			Object expr = TupleUtil.extractElement(step, 2, Object.class);

			if(estimator.getMiningFunction() != miningFunction){
				throw new IllegalArgumentException();
			}

			Predicate predicate = EvaluatableUtil.translatePredicate(expr, scope);

			Model model = estimator.encode(schema);

			Segment segment = new Segment(predicate, model)
				.setId(name);

			segmentation.addSegments(segment);
		}

		MiningModel miningModel = new MiningModel(miningFunction, ModelUtil.createMiningSchema(label))
			.setSegmentation(segmentation);

		MiningModelUtil.optimizeOutputFields(miningModel);

		return miningModel;
	}
}