import React from "react";

import DefaultLayout from "./DefaultLayout";
import FloatingChatbotWidget from "../pages/aiSecretary/components/FloatingChatbotWidget";

const UserAppLayout = ({ userInfo }) => {
  return (
    <>
      <DefaultLayout userInfo={userInfo} />
      <FloatingChatbotWidget userInfo={userInfo} />
    </>
  );
};

export default UserAppLayout;
