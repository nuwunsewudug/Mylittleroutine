package com.example.realtodoapp.ui

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.DialogInterestProgressBinding
import com.example.realtodoapp.databinding.DialogSigninBinding
import com.example.realtodoapp.databinding.FragmentCommunityBinding
import com.example.realtodoapp.databinding.FragmentLoginBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.model.MemberInfoDto
import com.example.realtodoapp.util.RetrofitUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.streams.toList

class LoginFragment: Fragment() {
    lateinit var fragmentLoginBinding: FragmentLoginBinding
    lateinit var dialogSigninBinding: DialogSigninBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        fragmentLoginBinding = FragmentLoginBinding.inflate(layoutInflater)
        dialogSigninBinding = DialogSigninBinding.inflate(layoutInflater)

        // 저장된 id 정보 있을 시 자동 로그인
        // 저장된 id 정보 불러오기
        var loginMemberInfo = MemberInfoDto()
        var emptyLoginMemberInfo = gson.toJson(loginMemberInfo)
        var loginMemberInfoJson = sharedPref.getString("loginMemberInfo",emptyLoginMemberInfo).toString()
        loginMemberInfo = gson.fromJson(loginMemberInfoJson)

        RetrofitUtil.login(
            loginMemberInfo.mem_id, loginMemberInfo.mem_pw,
            successCallback = {
                Toast.makeText(requireContext(), "로그인 성공하였습니다.", Toast.LENGTH_SHORT).show()

                // 기기에 로그인 정보 저장
                var loginMemberInfoJson = gson.toJson(it)
                sharedPrefEditor.putString("loginMemberInfo", loginMemberInfoJson)
                sharedPrefEditor.commit()

                // 화면 이동
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            },
            failCallback = {
                Toast.makeText(requireContext(), "로그인 실패하였습니다. 다시 로그인해주세요", Toast.LENGTH_SHORT).show()
            }
        )

        fragmentLoginBinding.buttonLogin.setOnClickListener(){
            var id = fragmentLoginBinding.editId.text.toString()
            var pw = fragmentLoginBinding.editPassword.text.toString()

            RetrofitUtil.login(
                id, pw,
                successCallback = {
                    Toast.makeText(requireContext(), "로그인 성공하였습니다.", Toast.LENGTH_SHORT).show()

                    // 기기에 로그인 정보 저장
                    var loginMemberInfoJson = gson.toJson(it)
                    sharedPrefEditor.putString("loginMemberInfo", loginMemberInfoJson)
                    sharedPrefEditor.commit()

                    // 화면 이동
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                },
                failCallback = {
                    Toast.makeText(requireContext(), "로그인 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        fragmentLoginBinding.buttonSignin.setOnClickListener(){
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialogSigninBinding.root.parent != null) {
                (dialogSigninBinding.root.parent as ViewGroup).removeView(
                    dialogSigninBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogSigninBinding.root)
            var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = (requireContext().getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (requireContext().getResources()
                .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setAttributes(params)
            dialog.getWindow()!!.setGravity(Gravity.CENTER)
            dialog.setCancelable(true)
            dialog.show()

            dialogSigninBinding.okButton.setOnClickListener(){
                var id = dialogSigninBinding.editId.text.toString()
                var pw = dialogSigninBinding.editPassword.text.toString()
                var name = dialogSigninBinding.editName.text.toString()

                RetrofitUtil.signIn(id, pw, name,
                    successCallback = {
                        Toast.makeText(requireContext(), "회원가입 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        if (dialogSigninBinding.root.parent != null) {
                            (dialogSigninBinding.root.parent as ViewGroup).removeView(
                                dialogSigninBinding.root
                            ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                            dialog.dismiss()
                        }
                    },
                    failCallback = {
                        Toast.makeText(requireContext(), "회원가입 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            dialogSigninBinding.noButton.setOnClickListener(){
                if (dialogSigninBinding.root.parent != null) {
                    (dialogSigninBinding.root.parent as ViewGroup).removeView(
                        dialogSigninBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
            }
        }



        val view = fragmentLoginBinding.root
        return view
    }
}