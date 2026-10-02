package com.example.coursey.core.di

import android.content.Context
import com.example.coursey.core.database.AppDatabase
import com.example.coursey.core.mock.AssetReader
import com.example.coursey.core.mock.AssetReaderImpl
import com.example.coursey.core.mock.MockAssets
import com.example.coursey.core.network.NetworkInfo
import com.example.coursey.core.network.NetworkInfoImpl
import com.example.coursey.features.auth.data.datasource.AuthLocalDataSource
import com.example.coursey.features.auth.data.datasource.AuthLocalDataSourceImpl
import com.example.coursey.features.auth.data.datasource.AuthMockRemoteDataSource
import com.example.coursey.features.auth.data.datasource.AuthRemoteDataSource
import com.example.coursey.features.auth.data.repository.AuthRepositoryImpl
import com.example.coursey.features.auth.domain.repository.AuthRepository
import com.example.coursey.features.auth.domain.usecase.GetLoggedInUserUseCase
import com.example.coursey.features.auth.domain.usecase.LoginUseCase
import com.example.coursey.features.auth.domain.usecase.LogoutUseCase
import com.example.coursey.features.course.data.datasource.CourseLocalDataSource
import com.example.coursey.features.course.data.datasource.CourseLocalDataSourceImpl
import com.example.coursey.features.course.data.datasource.CourseMockRemoteDataSource
import com.example.coursey.features.course.data.datasource.CourseRemoteDataSource
import com.example.coursey.features.course.data.repository.CourseRepositoryImpl
import com.example.coursey.features.course.domain.repository.CourseRepository
import com.example.coursey.features.course.domain.usecase.CompleteLessonUseCase
import com.example.coursey.features.course.domain.usecase.ObserveCourseDetailsUseCase
import com.example.coursey.features.course.domain.usecase.ObserveCoursesUseCase
import com.example.coursey.features.course.domain.usecase.RefreshCourseDetailsUseCase
import com.example.coursey.features.course.domain.usecase.RefreshCoursesUseCase

class AppContainer(context: Context) {

    private val networkInfo: NetworkInfo = NetworkInfoImpl(context)

    private val assetReader: AssetReader = AssetReaderImpl(context)

    private val database = AppDatabase(context)

    private val courseRemoteDataSource: CourseRemoteDataSource = CourseMockRemoteDataSource(
        assetReader = assetReader,
        coursesPath = MockAssets.COURSES,
        lessonsPath = MockAssets::lessons,
    )

    private val courseLocalDataSource: CourseLocalDataSource = CourseLocalDataSourceImpl(database)

    private val courseRepository: CourseRepository =
        CourseRepositoryImpl(courseRemoteDataSource, courseLocalDataSource, networkInfo)

    private val authPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val authRemoteDataSource: AuthRemoteDataSource = AuthMockRemoteDataSource()

    private val authLocalDataSource: AuthLocalDataSource = AuthLocalDataSourceImpl(authPreferences)

    private val authRepository: AuthRepository = AuthRepositoryImpl(
        remote = authRemoteDataSource,
        local = authLocalDataSource,
        networkInfo = networkInfo,
        userDataCleaners = listOf(courseLocalDataSource),
    )

    val loginUseCase: LoginUseCase get() = LoginUseCase(authRepository)

    val getLoggedInUserUseCase: GetLoggedInUserUseCase get() = GetLoggedInUserUseCase(authRepository)

    val logoutUseCase: LogoutUseCase get() = LogoutUseCase(authRepository)

    val observeCoursesUseCase: ObserveCoursesUseCase get() = ObserveCoursesUseCase(courseRepository)

    val refreshCoursesUseCase: RefreshCoursesUseCase get() = RefreshCoursesUseCase(courseRepository)

    val observeCourseDetailsUseCase: ObserveCourseDetailsUseCase
        get() = ObserveCourseDetailsUseCase(courseRepository)

    val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase
        get() = RefreshCourseDetailsUseCase(courseRepository)

    val completeLessonUseCase: CompleteLessonUseCase get() = CompleteLessonUseCase(courseRepository)
}
