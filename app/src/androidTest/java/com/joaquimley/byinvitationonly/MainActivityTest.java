/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 3, 29 June 2007
 *
 *     Copyright (c) 2015 Joaquim Ley <me@joaquimley.com>
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.joaquimley.byinvitationonly;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.firebase.client.Firebase;
import com.joaquimley.byinvitationonly.activities.EditUserDetailsActivity;
import com.joaquimley.byinvitationonly.activities.MainActivity;
import com.joaquimley.byinvitationonly.activities.ParticipantsListActivity;
import com.joaquimley.byinvitationonly.helper.FirebaseHelper;
import com.joaquimley.byinvitationonly.model.User;
import com.robotium.solo.Solo;


public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    protected static final String TAG = MainActivityTest.class.getSimpleName();
    protected static final String NO_MSG_ERROR = "No error message displayed";
    protected static final String WRONG_ACTIVITY_ERROR = "ERROR: Not expected activity showing";

    private MainActivity mActivity;
    protected Solo solo;
    // Views
    protected ImageButton mBtnStatus;
    protected ImageButton mBtnEditUser;
    protected Button mParticipantsListActivity;
    // Objects
    protected User mUser;
    private EditText mEditTestUserName;
    private EditText mEditTestUserEmail;
    private EditText mEditTestUserDescription;
    private SharedPreferences mSharedPreferences;
    private Firebase mSessionsRef;
    private Firebase mUsersRef;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    /**
     * On the start of the testing
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation());
        setActivityInitialTouchMode(true);
        mActivity = getActivity();
        mUser = BioApp.getInstance().getCurrentUser();
        init();
    }

    /**
     * On finish
     *
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    /**
     * Initialisation of UI components
     */
    public void init() {
        Firebase firebaseRef = FirebaseHelper.initiateFirebase(mActivity);
        mSessionsRef = FirebaseHelper.getChildRef(firebaseRef, mActivity.getString(R.string.firebase_child_sessions));
        mUsersRef = FirebaseHelper.getChildRef(firebaseRef, mActivity.getString(R.string.firebase_child_users));

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mEditTestUserName = (EditText) mActivity.findViewById(R.id.et_edit_user_details_name);
        mEditTestUserEmail = (EditText) mActivity.findViewById(R.id.et_edit_user_details_email);
        mEditTestUserDescription = (EditText) mActivity.findViewById(R.id.et_edit_user_details_description);
        preConditions();
    }

    /**
     * Testes if start Activity isn't null
     */
    public void preConditions() {
        assertNotNull(mActivity);
        assertTrue(WRONG_ACTIVITY_ERROR, solo.waitForActivity(MainActivity.class.getSimpleName()));
    }

    /**
     * Clears all sharedPreferences information on device, sets mUser to NULL
     */
    private void clearUserProfile() {
        mSharedPreferences.edit().clear().apply();
        mUser = null;
    }

    /**
     * Recreates user profile simulating user behavior
     */
    public void reCreateUserProfile(boolean forceUpdate) {
        if (!forceUpdate) {
            if (mUser != null || !mUser.getName().isEmpty()) {
                return;
            }
        }

        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForActivity(EditUserDetailsActivity.class);

        String newName = "Joaquim Ley";
        String newEmail = "me@joaquimley.com";
        String newDescription = "Android Developer\nGraphic Designer";

        init();
        // Name
        solo.clearEditText(mEditTestUserName);
        solo.typeText(mEditTestUserName, newName);
        // Email
        solo.clearEditText(mEditTestUserEmail);
        solo.typeText(mEditTestUserEmail, newEmail);
        // Description
        solo.clearEditText(mEditTestUserDescription);
        solo.typeText(mEditTestUserDescription, newDescription);

        solo.clickOnText(solo.getString(R.string.text_save));
        assertTrue(WRONG_ACTIVITY_ERROR, solo.waitForActivity(MainActivity.class.getSimpleName()));

        mUser = BioApp.getInstance().getCurrentUser();

        assertTrue(mUser.getName().equals(newName));
        assertTrue(mUser.getEmail().equals(newEmail));
        assertTrue(mUser.getDescription().equals(newDescription));
    }

    // -------------------------------------------------------------------------------------- //
    // --------------------------------------- Tests ---------------------------------------- //
    // -------------------------------------------------------------------------------------- //

    /**
     * Dado que estou na aplica��o, Quando pressiono o bot�o "I'm here" e as informa��es do meu
     * contacto est�o preenchidas (pelo menos nome e email), Ent�o deve ser-me exibida uma mensagem
     * de confirma��o da activa��o desta funcionalidade.
     */
    @SmallTest
    public void test01_Us4ShareDetails() {
        init();
        reCreateUserProfile(false);
        Drawable previousIcon = mActivity.findViewById(R.id.ib_user_status).getBackground();
        boolean prevStatus = mUser.isVisible();

        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForText(mActivity.getString(R.string.text_status_updated));
        assertTrue("User didn't change checked-in status", prevStatus != mUser.isVisible());
        assertTrue("User profile not present on server", BioApp.getInstance().getUsersList().contains(mUser));
        assertTrue(mActivity.findViewById(R.id.ib_user_status).getBackground() != previousIcon);
    }

    /**
     * Dado que estou na mensagem de confirma��o da funcionalidade "I'm here", Quando pressiono no
     * bot�o "Sim" e tenho rede, Ent�o os meus dados devem ser submetidos e partilhados com os
     * restantes participantes que activaram a funcionalidade "I'm here" e o bot�o deve mudar para
     * o estado activo.
     */
    @SmallTest
    public void test02_Us4NoUserProfileCheckIn() {
        init();
        reCreateUserProfile(false);

        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForText(mActivity.getString(R.string.text_status_updated));
        assertTrue("No status icon changed", mActivity.getMenu().getItem(0).getIcon() ==
                mActivity.getResources().getDrawable(R.drawable.ic_status_green));
    }

    /**
     * Dado que estou na aplica��o, Quando pressiono o bot�o "I'm here" e as informa��es do meu
     * contacto n�o est�o preenchidas (pelo menos nome e email), Ent�o deve ser-me exibida uma
     * mensagem perguntando se pretendo preencher as informa��es do meu contacto ou cancelar a ac��o.
     */
    @SmallTest
    public void test03_Us4CreateProfileMessage() {
        clearUserProfile();
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        assertTrue(solo.waitForText(mActivity.getString(R.string.error_must_create_profile_first)));
    }

    /**
     * Dado que estou a visualizar a mensagem perguntando se pretendo preencher as informa��es do
     * meu contacto, Quando selecciono "Sim", Ent�o devo ser reencaminhado para o ecr� de
     * preenchimento de detalhes do meu contacto.
     */
    @MediumTest
    public void test04_Us4CreateOrEditUserProfile() {
        clearUserProfile();
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForText(mActivity.getString(R.string.error_must_create_profile_first));
        solo.clickOnText("Yes");
        solo.waitForActivity(EditUserDetailsActivity.class);
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, EditUserDetailsActivity.class);
    }

    /**
     * Dado que estou a visualizar a mensagem perguntando se pretendo preencher as informa��es do
     * meu contacto, Quando selecciono "N�o", Ent�o deve ser exibido o ecr� inicial mantendo-se a
     * funcionalidade "I'm here" desactivada.
     */
    @MediumTest
    public void test05_Us4NoCreateProfile() {
        clearUserProfile();
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForText(mActivity.getString(R.string.error_must_create_profile_first));
        solo.clickOnText("No");
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
    }


    /**
     * Dado que estou no ecr� de preenchimento de detalhes do meu contacto, Quando termino o
     * preenchimento dos dados, pressiono o bot�o de back, e tenho as informa��es do meu contacto
     * preenchidas (pelo menos nome e email), Ent�o deve-me ser exibida uma mensagem de confirma��o
     * da activa��o desta funcionalidade.
     */
    @MediumTest
    public void test06_Us5CancelCreateProfile() {
        clearUserProfile();
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForText(mActivity.getString(R.string.error_must_create_profile_first));
        solo.clickOnText("Yes");
        solo.waitForActivity(EditUserDetailsActivity.class);
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, EditUserDetailsActivity.class);

        solo.clickOnText("Cancel");
        assertTrue(solo.waitForText(mActivity.getString(R.string.error_confirm_cancel)));
    }

    /**
     * Dado que estou no ecr� de preenchimento de detalhes do meu contacto, Quando termino o
     * preenchimento dos dados, pressiono o bot�o de back, e n�o tenho as informa��es do meu
     * contacto preenchidas (pelo menos nome e email), Ent�o deve ser exibido o ecr� inicial
     * mantendo-se a funcionalidade "I'm here" desactivada.
     */
    @MediumTest
    public void test07_Us4PressBackOnProfileCreate() {
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
        solo.waitForActivity(EditUserDetailsActivity.class);

        String newName = "Joaquim Ley";
        String newEmail = "me@joaquimley.com";
        String newDescription = "Android Developer\nGraphic Designer";

        init();
        // Name
        solo.clearEditText(mEditTestUserName);
        solo.typeText(mEditTestUserName, newName);
        // Email
        solo.clearEditText(mEditTestUserEmail);
        solo.typeText(mEditTestUserEmail, newEmail);
        // Description
        solo.clearEditText(mEditTestUserDescription);
        solo.typeText(mEditTestUserDescription, newDescription);

        solo.goBack();
        assertTrue(WRONG_ACTIVITY_ERROR, solo.waitForActivity(MainActivity.class.getSimpleName()));
        assertTrue(mActivity.getMenu().getItem(0).getIcon() == mActivity.getResources().getDrawable(R.drawable.ic_status_red));
    }

    /**
     * Dado que estou na mensagem de confirma��o da funcionalidade "I'm here", Quando pressiono no
     * bot�o "Sim" e n�o tenho rede, Ent�o deve-me ser apresentada uma mensagem informativa a
     * indicar a falta de rede.
     */
    @SmallTest
    public void test08_Us4CheckInNoConnection() {
        solo.setWiFiData(false);
        if (mUser.isVisible()) {
            solo.clickOnImageButton(1);
        }
        solo.clickOnImageButton(1);
        solo.waitForText(mActivity.getString(R.string.error_no_internet));
    }

    /**
     * Dado que estou no ecr� de preenchimento de detalhes do meu contacto, Quando termino o
     * preenchimento dos dados, pressiono o bot�o de back, e tenho as informa��es do meu contacto
     * preenchidas (pelo menos nome e email), Ent�o deve-me ser exibida uma mensagem de confirma��o
     * da activa��o desta funcionalidade.
     */
    @MediumTest
    public void test09_Us6ProfileUpdateConfirmation() {
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, MainActivity.class);
        reCreateUserProfile(true);
        solo.waitForText(mActivity.getString(R.string.text_profile_updated));
        assertTrue(WRONG_ACTIVITY_ERROR, solo.waitForActivity(MainActivity.class));
    }

    /**
     * Dado que estou no ecr� da lista de participantes, quando fa�o "pull to refresh" e tenho
     * rede, a lista de participantes deve ser actualizada.
     */
    @SmallTest
    public void test10_Us6PullToRefreshParticipantsUi() {
        if(!mUser.isVisible()){
            solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
            solo.waitForActivity(mActivity.getString(R.string.text_status_updated));
        }

        mActivity.getNavigationDrawerFragment().openDrawer();
        solo.clickOnText(mActivity.getString(R.string.title_activity_participants_list));
        assertTrue(solo.waitForActivity(ParticipantsListActivity.class));
        solo.clickLongInList(1, 4, 111);
    }

    /**
     * Dado que estou no ecr� da lista de participantes, Quando n�o tenho rede, ao fazer
     * "pulltorefresh", dever� aparecer uma mensagem a dizer que n�o est� conectado � rede.
     * A lista de participantes deve permanecer sem altera��es.
     */
    @MediumTest
    public void test11_Us4UserDetailsEdited() {
        if(!mUser.isVisible()){
            solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
            solo.waitForActivity(mActivity.getString(R.string.text_status_updated));
        }

        mActivity.getNavigationDrawerFragment().openDrawer();
        solo.clickOnText(mActivity.getString(R.string.title_activity_participants_list));
        assertTrue(solo.waitForActivity(ParticipantsListActivity.class));
        solo.setWiFiData(false);
        solo.clickLongInList(1, 4, 111);
        assertTrue(solo.waitForText(mActivity.getString(R.string.error_no_internet)));
    }

    /**
     * Dado que estou no ecr� da lista de participantes, Quando escolho um participante para
     * contactar, Ent�o deve ser-me exibida uma mensagem de confirma��o de contacto a esse
     * participante.
     */
    @SmallTest
    public void test12_Us6ConfirmContactUser(){
        if(!mUser.isVisible()){
            solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
            solo.waitForActivity(mActivity.getString(R.string.text_status_updated));
        }

        mActivity.getNavigationDrawerFragment().openDrawer();
        solo.clickOnText(mActivity.getString(R.string.title_activity_participants_list));
        assertTrue(solo.waitForActivity(ParticipantsListActivity.class));
        solo.clickInList(2);
        assertTrue(solo.waitForText(mActivity.getString(R.string.text_confirm_contact_user)));
    }
    /**
     * Dado que estou na mensagem de confirma��o de contacto a um participante, Quando pressiono o
     * bot�o "Sim" e n�o tenho rede, Ent�o deve-me ser apresentada uma mensagem informativa a
     * indicar a falta de rede.
     */
    @SmallTest
    public void test13_Us6NoWifiContactUser(){
        if(!mUser.isVisible()){
            solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
            solo.waitForActivity(mActivity.getString(R.string.text_status_updated));
        }

        mActivity.getNavigationDrawerFragment().openDrawer();
        solo.clickOnText(mActivity.getString(R.string.title_activity_participants_list));
        assertTrue(solo.waitForActivity(ParticipantsListActivity.class));
        solo.setWiFiData(false);
        solo.clickInList(2);
        assertTrue(solo.waitForText(mActivity.getString(R.string.error_no_internet)));
    }

    /**
     * Dado que estou na mensagem de confirma��o de contacto a um participante, Quando pressiono o
     * bot�o "N�o", Ent�o deve ser exibido o ecr� da lista de participantes.
     */
    @SmallTest
    public void test14_Us6NoConfirmContactUser(){
        if(!mUser.isVisible()){
            solo.clickOnMenuItem(mActivity.getString(R.string.action_check_in));
            solo.waitForActivity(mActivity.getString(R.string.text_status_updated));
        }

        mActivity.getNavigationDrawerFragment().openDrawer();
        solo.clickOnText(mActivity.getString(R.string.title_activity_participants_list));
        assertTrue(solo.waitForActivity(ParticipantsListActivity.class));
        solo.setWiFiData(false);
        solo.clickInList(2);
        assertTrue(solo.waitForText(mActivity.getString(R.string.text_confirm_contact_user)));
        solo.clickOnText("No");
        solo.waitForDialogToClose();
        solo.assertCurrentActivity(WRONG_ACTIVITY_ERROR, ParticipantsListActivity.class);
    }
}