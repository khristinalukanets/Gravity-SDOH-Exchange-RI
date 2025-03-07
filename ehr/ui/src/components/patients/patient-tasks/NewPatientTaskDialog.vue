<script lang="ts">
import { defineComponent, ref, computed, watch, reactive } from "vue";
import { RuleItem } from "async-validator";
import { TasksModule } from "@/store/modules/tasks";
import { PatientTasksModule } from "@/store/modules/patientTasks";
import { Task, NewPatientTaskPayload } from "@/types";
import { prepareOccurrence } from "@/utils/utils";
import { getAssessments } from "@/api";

type FormModel = {
	name: string,
	type: TaskType | "",
	code: string,
	status: string,
	priority: string,
	occurrence: "",
	comment: string,
	// additional type related fields
	questionnaireType: string,
	questionnaireFormat: string,
	questionnaireId: string,
	referralTaskId: string
};

const DEFAULT_REQUIRED_RULE = {
	required: true,
	message: "This field is required"
};

type TaskType = "MAKE_CONTACT" | "COMPLETE_SR_QUESTIONNAIRE" | "SERVICE_FEEDBACK";

const TYPE_CODE_MAP: Record<TaskType, { code: string, display: string }> = {
	MAKE_CONTACT: {
		code: "make-contact",
		display: "Make Contact"
	},
	COMPLETE_SR_QUESTIONNAIRE: {
		code: "complete-questionnaire",
		display: "Complete Questionnaire"
	},
	SERVICE_FEEDBACK: {
		code: "complete-questionnaire",
		display: "Complete Questionnaire"
	}
};

export default defineComponent({
	props: {
		visible: {
			type: Boolean,
			default: false
		}
	},
	emits: ["close"],
	setup(_, { emit }) {
		const saveInProgress = ref<boolean>(false);
		const occurrenceType = ref<string>("");
		const typeOptions = ref<{ label: string, value: TaskType }[]>([{
			label: "Complete questionnaire regarding social risks",
			value: "COMPLETE_SR_QUESTIONNAIRE"
		}, {
			label: "Provide feedback on service delivered",
			value: "SERVICE_FEEDBACK"
		}]);
		const statusOptions = ref<{ label: string, value: string }[]>([{
			label: "Ready",
			value: "ready"
		}]);
		const questionnaireOptions = ref<{ label: string, value: string }[]>([]);
		const formModel = reactive<FormModel>({
			name: "",
			type: "",
			code: "",
			status: "Ready",
			priority: "Routine",
			occurrence: "",
			comment: "",
			questionnaireType: "",
			questionnaireFormat: "FHIR_QUESTIONNAIRE",
			questionnaireId: "",
			referralTaskId: ""
		});
		const formEl = ref<HTMLFormElement>();
		const formRules: { [field: string]: RuleItem & { trigger?: string } } = {
			name: DEFAULT_REQUIRED_RULE,
			type: DEFAULT_REQUIRED_RULE,
			priority: DEFAULT_REQUIRED_RULE,
			occurrence: DEFAULT_REQUIRED_RULE,
			questionnaireFormat: DEFAULT_REQUIRED_RULE,
			questionnaireId: DEFAULT_REQUIRED_RULE,
			referralTaskId: DEFAULT_REQUIRED_RULE
		};
		const formHasChanges = computed<boolean>(() =>
			(
				formModel.name !== "" ||
				formModel.type !== "" ||
				formModel.code !== "" ||
				formModel.status !== "Ready" ||
				formModel.priority !== "Routine" ||
				formModel.occurrence !== "" ||
				formModel.comment !== ""
			)
		);
		const referralTasks = computed<Task[]>(() => TasksModule.tasks);
		const referralTaskOptions = computed<{ label: string, value: string }[]>(() => referralTasks.value.map(t => ({
			label: t.name,
			value: t.id
		})));

		watch(() => formModel.type, val => {
			if (val === "COMPLETE_SR_QUESTIONNAIRE" || val === "SERVICE_FEEDBACK") {
				formModel.code = TYPE_CODE_MAP[val].display;
				formModel.questionnaireType = "Risk Questionnaire";
			}
		});

		const onDialogClose = () => {
			formEl.value?.resetFields();
			occurrenceType.value = "";
			emit("close");
		};
		const onDialogOpen = async () => {
			// to show inside referrals dropdown
			await TasksModule.getTasks();
			const assessments = await getAssessments();
			questionnaireOptions.value = assessments.map(a => ({
				label: a.display,
				value: a.id
			}));
		};
		const onFormSave = async () => {
			await formEl.value?.validate();
			saveInProgress.value = true;
			try {
				const payload: NewPatientTaskPayload = {
					code: "complete-questionnaire",
					comment: formModel.comment,
					name: formModel.name,
					occurrence: prepareOccurrence(formModel.occurrence),
					priority: formModel.priority,
					type: formModel.type
				};

				if (formModel.type === "COMPLETE_SR_QUESTIONNAIRE") {
					payload.questionnaireType = "RISK_QUESTIONNAIRE";
					payload.questionnaireFormat = formModel.questionnaireFormat;
					payload.questionnaireId = formModel.questionnaireId;
				}
				if (formModel.type === "SERVICE_FEEDBACK") {
					payload.referralTaskId = formModel.referralTaskId;
				}

				await PatientTasksModule.createPatientTask(payload);
				emit("close");
			} finally {
				saveInProgress.value = false;
			}
		};

		//
		// On every until/from...to change clear model, element-ui can't work with array or date in both datepicker and range datepicker.
		//
		const onOccurrenceSelectChange = () => {
			formModel.occurrence = "";
		};

		//
		// Disable all dates that are less than today. Used inside occurrence date-pickers.
		//
		const disabledOccurrenceDate = (time: Date): boolean => time.getTime() < Date.now();

		return {
			onDialogClose,
			onDialogOpen,
			formEl,
			saveInProgress,
			onFormSave,
			formModel,
			formRules,
			typeOptions,
			statusOptions,
			formHasChanges,
			questionnaireOptions,
			referralTaskOptions,
			occurrenceType,
			onOccurrenceSelectChange,
			disabledOccurrenceDate
		};
	}
});
</script>

<template>
	<el-dialog
		:model-value="visible"
		title="Add Patient Task"
		:width="700"
		destroy-on-close
		custom-class="patient-task-dialog"
		@close="onDialogClose"
		@open="onDialogOpen"
	>
		<el-form
			ref="formEl"
			:model="formModel"
			:rules="formRules"
			label-width="160px"
			label-position="left"
			size="mini"
			class="new-patient-task-form"
		>
			<el-form-item
				label="Task Name"
				prop="name"
			>
				<el-input
					v-model="formModel.name"
					placeholder="Add name"
				/>
			</el-form-item>
			<el-form-item
				label="Type"
				prop="type"
			>
				<el-select
					v-model="formModel.type"
					placeholder="Select type"
				>
					<el-option
						v-for="item in typeOptions"
						:key="item.value"
						:label="item.label"
						:value="item.value"
					/>
				</el-select>
			</el-form-item>

			<template v-if="formModel.type === 'SERVICE_FEEDBACK'">
				<el-form-item
					label="Referral Task"
					prop="referralTaskId"
				>
					<el-select
						v-model="formModel.referralTaskId"
						placeholder="Select referral task"
					>
						<el-option
							v-for="item in referralTaskOptions"
							:key="item.value"
							:label="item.label"
							:value="item.value"
						/>
					</el-select>
				</el-form-item>
			</template>

			<el-form-item
				label="Code"
				prop="code"
			>
				<el-input
					v-model="formModel.code"
					placeholder="Select code"
					:disabled="true"
				/>
			</el-form-item>

			<template v-if="formModel.type === 'COMPLETE_SR_QUESTIONNAIRE'">
				<el-form-item
					label="Questionnaire Type"
					prop="questionnaireType"
				>
					<el-input
						v-model="formModel.questionnaireType"
						:disabled="true"
					/>
				</el-form-item>
				<el-form-item
					label="Questionnaire Format"
					prop="questionnaireFormat"
				>
					<el-radio-group v-model="formModel.questionnaireFormat">
						<el-radio label="FHIR_QUESTIONNAIRE">
							FHIR Questionnaire
						</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item
					label="FHIR Questionnaire"
					prop="questionnaireId"
				>
					<el-select
						v-model="formModel.questionnaireId"
						placeholder="Select questionnaire"
					>
						<el-option
							v-for="item in questionnaireOptions"
							:key="item.value"
							:label="item.label"
							:value="item.value"
						/>
					</el-select>
				</el-form-item>
			</template>

			<el-divider />

			<el-form-item
				label="Status"
				prop="status"
			>
				<el-select
					v-model="formModel.status"
					placeholder="Select status"
					class="half"
					:disabled="true"
				>
					<el-option
						v-for="item in statusOptions"
						:key="item.value"
						:label="item.label"
						:value="item.value"
					/>
				</el-select>
			</el-form-item>
			<el-form-item
				label="Priority"
				prop="priority"
			>
				<el-radio-group v-model="formModel.priority">
					<el-radio label="Routine" />
					<el-radio label="Urgent" />
					<el-radio label="ASAP" />
				</el-radio-group>
			</el-form-item>
			<el-form-item
				label="Occurrence"
				prop="occurrence"
			>
				<el-select
					v-model="occurrenceType"
					placeholder="Select"
					class="small"
					@change="onOccurrenceSelectChange"
				>
					<el-option
						label="Until"
						value="until"
					/>
					<el-option
						label="From...to"
						value="range"
					/>
				</el-select>
				<el-date-picker
					v-if="occurrenceType === 'until'"
					v-model="formModel.occurrence"
					:disabled-date="disabledOccurrenceDate"
					placeholder="Select date"
				/>
				<el-date-picker
					v-if="occurrenceType === 'range'"
					v-model="formModel.occurrence"
					type="daterange"
					range-separator="To"
					start-placeholder="Select date"
					end-placeholder="Select date"
					:disabled-date="disabledOccurrenceDate"
				/>
			</el-form-item>
			<el-form-item
				label="Comment"
				prop="comment"
			>
				<el-input
					v-model="formModel.comment"
					type="textarea"
					rows="2"
					placeholder="Enter your comment here..."
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button
				round
				size="mini"
				@click="onDialogClose"
			>
				Cancel
			</el-button>
			<el-button
				plain
				round
				type="primary"
				size="mini"
				:loading="saveInProgress"
				:disabled="!formHasChanges"
				@click="onFormSave"
			>
				Create
			</el-button>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped>
@import "~@/assets/scss/abstracts/variables";

.new-patient-task-form {
	.el-select {
		width: 100%;

		&.half {
			width: 50%;
			margin-right: 15px;
		}

		&.small {
			width: 20%;
			margin-right: 15px;
		}
	}

	.el-divider {
		margin: 20px 0;
	}
}

::v-deep(.el-date-editor.el-input) {
	width: 125px;
}

</style>
