/*
 * Copyright (c) 2023 Villu Ruusmann
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
package sklearn.calibration;

import java.util.Collections;
import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.python.FunctionUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Regressor;
import sklearn.Transformer;

public class SigmoidCalibration extends Regressor {

	public SigmoidCalibration(String module, String name){
		super(module, name);
	}

	@Override
	public Model encodeModel(Schema schema){
		throw new UnsupportedOperationException();
	}

	/**
	 * @see Transformer#encodeFeatures(List, SkLearnEncoder)
	 */
	public List<Feature> encodeFeatures(List<Feature> features, SkLearnEncoder encoder){
		Number a = getA();
		Number b = getB();

		SchemaUtil.checkSize(1, features);

		Feature feature = features.get(0);

		Apply apply = PMMLUtil.createApply(PMMLFunctions.MULTIPLY,
			PMMLUtil.createConstant(-1),
			PMMLUtil.createApply(PMMLFunctions.ADD,
				PMMLUtil.createApply(PMMLFunctions.MULTIPLY, PMMLUtil.createConstant(a), feature.ref()),
				PMMLUtil.createConstant(b)
			)
		);

		apply = FunctionUtil.encodeScipyFunction("scipy.special", "expit", Collections.singletonList(apply));

		DerivedField derivedField = encoder.createDerivedField(createFieldName("sigmoidCalibration", feature), OpType.CONTINUOUS, DataType.DOUBLE, apply);

		return Collections.singletonList(new ContinuousFeature(encoder, derivedField));
	}

	public Number getA(){
		return getNumber("a_");
	}

	public Number getB(){
		return getNumber("b_");
	}
}