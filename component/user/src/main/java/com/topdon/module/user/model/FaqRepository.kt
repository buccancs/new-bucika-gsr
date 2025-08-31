package com.topdon.module.user.model

import com.blankj.utilcode.util.Utils
import com.topdon.module.user.R

object FaqRepository {

    fun getQuestionList(isTS001: Boolean): ArrayList<QuestionData> = if (isTS001) arrayListOf(
        QuestionData(
            question = Utils.getApp().getString(R.string.question1),
            answer = Utils.getApp().getString(R.string.answer1)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question2),
            answer = Utils.getApp().getString(R.string.answer2)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question3),
            answer = Utils.getApp().getString(R.string.answer3)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question4),
            answer = Utils.getApp().getString(R.string.answer4)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question5),
            answer = Utils.getApp().getString(R.string.answer5)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question6),
            answer = Utils.getApp().getString(R.string.answer6)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question7),
            answer = Utils.getApp().getString(R.string.answer7)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.question8),
            answer = Utils.getApp().getString(R.string.answer8)
        )
    ) else arrayListOf(
        QuestionData(
            question = Utils.getApp().getString(R.string.ts004_faq_q1),
            answer = Utils.getApp().getString(R.string.ts004_faq_a1)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.ts004_faq_q2),
            answer = Utils.getApp().getString(R.string.ts004_faq_a2)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.ts004_faq_q3),
            answer = Utils.getApp().getString(R.string.ts004_faq_a3)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.ts004_faq_q4),
            answer = Utils.getApp().getString(R.string.ts004_faq_a4)
        ),
        QuestionData(
            question = Utils.getApp().getString(R.string.ts004_faq_q5),
            answer = Utils.getApp().getString(R.string.ts004_faq_a5)
        ),
    )
}

data class QuestionData(
    val question: String,
    val answer: String
)