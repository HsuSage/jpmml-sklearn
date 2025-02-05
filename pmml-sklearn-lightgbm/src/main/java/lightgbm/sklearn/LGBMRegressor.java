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
package lightgbm.sklearn;

import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.Label;
import org.jpmml.converter.Schema;
import org.jpmml.lightgbm.HasLightGBMOptions;
import org.jpmml.lightgbm.ObjectiveFunction;
import org.jpmml.lightgbm.Regression;
import sklearn.Regressor;

public class LGBMRegressor extends Regressor implements HasBooster, HasLightGBMOptions {

	public LGBMRegressor(String module, String name){
		super(module, name);
	}

	@Override
	public int getNumberOfFeatures(){
		return BoosterUtil.getNumberOfFeatures(this);
	}

	@Override
	public void checkLabel(Label label){
		ObjectiveFunction objectiveFunction = BoosterUtil.getObjectiveFunction(this);

		super.checkLabel(label);

		if((objectiveFunction != null) && !(objectiveFunction instanceof Regression)){
			throw new IllegalArgumentException("Expected a regression-type objective function, got '" + objectiveFunction.getName() + "'");
		}
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		return BoosterUtil.encodeModel(this, schema);
	}

	@Override
	public Booster getBooster(){
		return get("_Booster", Booster.class);
	}
}