/*
 * Copyright (c) 2022 Villu Ruusmann
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
package sklearn.multioutput;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.Model;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MultiLabel;
import org.jpmml.converter.ScalarLabel;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.python.ClassDictUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Estimator;

public class ChainUtil {

	private ChainUtil(){
	}

	static
	public <E extends Estimator> MiningModel encodeChain(List<E> estimators, List<Integer> order, Schema schema){
		ClassDictUtil.checkSize(estimators, order);

		SkLearnEncoder encoder = (SkLearnEncoder)schema.getEncoder();
		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		List<Model> models = new ArrayList<>();

		MultiLabel multiLabel = (MultiLabel)label;

		List<Feature> augmentedFeatures = new ArrayList<>(features);

		for(int i = 0; i < estimators.size(); i++){
			E estimator = estimators.get(i);

			if(order.get(i) != i){
				throw new IllegalArgumentException();
			}

			ScalarLabel scalarLabel = (ScalarLabel)multiLabel.getLabel(i);

			Schema segmentSchema = new Schema(encoder, scalarLabel, augmentedFeatures);

			Model model = estimator.encodeModel(segmentSchema);

			models.add(model);

			Feature feature = encoder.exportPrediction(model, scalarLabel);

			augmentedFeatures.add(feature);
		}

		return MiningModelUtil.createMultiModelChain(models, Segmentation.MissingPredictionTreatment.CONTINUE);
	}
}